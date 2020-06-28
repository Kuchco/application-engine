pipeline {
  agent any
    tools {
    maven 'localMaven3'
    jdk 'localJava8'
  }
  options {
    copyArtifactPermission('*');
  }
  environment {
        NEXUS_CRED = credentials('1986c778-eba7-44d7-b6f6-71e73906d894')
        pom = readMavenPom()
  }

  stages {

    stage('Prepare') {
        steps {
            bitbucketStatusNotify(buildState: 'INPROGRESS')
            echo "‎ _   _        _                 _   __ \n| \\ | |  ___ | |_   __ _  _ __ (_) / _|\n|  \\| | / _ \\| __| / _` || '__|| || |_	\n| |\\  ||  __/| |_ | (_| || |   | ||  _|\n|_| \\_| \\___| \\__| \\__, ||_|   |_||_|	\n                   |___/				\n\n\n"
            echo pom.getName()
            echo pom.getVersion()
            echo pom.getDescription()
        }
    }

    stage('Tests'){
        steps {
            echo 'Run tests'
        }
    }

    stage('Sonar') {
        steps {
            echo 'Sonar'
            withSonarQubeEnv('SonarNetgrif') {
                sh "mvn -DskipTests=true clean package sonar:sonar"
            }
        }
        post {
            success {
                echo '--------------------------------------------------------------------------------------------------------'
                echo 'Sonar SUCCESS'
                echo '--------------------------------------------------------------------------------------------------------'
            }
            failure {
                bitbucketStatusNotify(buildState: 'FAILED')
            }

        }
    }

    stage('Build') {
      steps {
        sh "mvn -DskipTests=true clean package install"

      }
      post {
        success {
          echo '--------------------------------------------------------------------------------------------------------'
          echo 'BUILD SUCCESS'
          echo '--------------------------------------------------------------------------------------------------------'
        }
        failure {
              bitbucketStatusNotify(buildState: 'FAILED')
        }
      }
    }

    stage('Documentation') {
        parallel {
            stage('JavaDoc') {
                echo 'Uploading JavaDoc to developer.netgrif.com'
                sshPublisher(
                    publishers: [
                        sshPublisherDesc(
                            configName: 'developer.netgrif.com',
                            transfers: [
                                sshTransfer(
                                    cleanRemote: true,
                                    excludes: '',
                                    execCommand: '',
                                    execTimeout: 120000,
                                    flatten: false,
                                    makeEmptyDirs: false,
                                    noDefaultExcludes: false,
                                    patternSeparator: '[, ]+',
                                    remoteDirectory: "/var/www/html/developer/projects/engine-backend/${pom.getVersion()}/javadoc",
                                    remoteDirectorySDF: false,
                                    removePrefix: 'target/apidocs',
                                    sourceFiles: 'target/apidocs/**')],
                            usePromotionTimestamp: false,
                            useWorkspaceInPromotion: false,
                            verbose: true)])
            }

            stage('GroovyDoc') {
                echo 'Building GroovyDoc'
            }

            stage('Swagger') {
                echo 'Building OpenApi 3 documentation'
            }

            stage('XSD Schema') {
                echo 'Publishing Petriflow XSD schema'
                sshPublisher(
                    publishers: [
                        sshPublisherDesc(
                            configName: 'developer.netgrif.com',
                            transfers: [
                                sshTransfer(
                                    cleanRemote: true,
                                    excludes: '',
                                    execCommand: '',
                                    execTimeout: 120000,
                                    flatten: false,
                                    makeEmptyDirs: false,
                                    noDefaultExcludes: false,
                                    patternSeparator: '[, ]+',
                                    remoteDirectory: "/var/www/html/developer/projects/engine-backend/${pom.getVersion()}/schema",
                                    remoteDirectorySDF: false,
                                    removePrefix: 'src/main/resources/petriNets',
                                    sourceFiles: 'src/main/resources/petriNets/petriflow_schema.xsd')],
                            usePromotionTimestamp: false,
                            useWorkspaceInPromotion: false,
                            verbose: true)])
            }
        }
    }

    stage('Nexus') {
        steps {

        }
    }

    stage('ZIP file') {
      steps {
        script {
            DATETIME_TAG = java.time.LocalDateTime.now()
            ZIP_FILE = "NETGRIF-${pom.getName().replace(' ','_')}-${pom.getVersion()}-Backend-${DATETIME_TAG}.zip"
        }
        sh '''
            mkdir dist
            cp target/*.jar dist/
        '''
        zip zipFile: ZIP_FILE, archive: false, dir: 'dist'
        archiveArtifacts artifacts:ZIP_FILE, fingerprint: true
      }
    }

    stage('Publish') {
        parallel {
            stage('Nexus') {
                steps {
                    echo 'Publishing to Nexus Maven repository'
                }
            }
        }
    }
  }

  post {
      always {
          //slackSend channel: '#ops-room',
          //          color: 'good',
          //          message: "The pipeline ${currentBuild.fullDisplayName} completed successfully."

          //junit 'coverage/netgrif-application-engine/JUNITX-test-report.xml'
      }

      success {
          bitbucketStatusNotify(buildState: 'SUCCESSFUL')
      }

      unstable {
          bitbucketStatusNotify(buildState: 'SUCCESSFUL')
      }

      failure {
          bitbucketStatusNotify(buildState: 'FAILED')
      }
    }
}