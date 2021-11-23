package com.netgrif.workflow.workflow.service;

import com.netgrif.workflow.auth.domain.LoggedUser;
import com.netgrif.workflow.auth.domain.User;
import com.netgrif.workflow.petrinet.domain.I18nString;
import com.netgrif.workflow.petrinet.domain.PetriNet;
import com.netgrif.workflow.petrinet.domain.dataset.EnumerationMapField;
import com.netgrif.workflow.petrinet.domain.dataset.MultichoiceMapField;
import com.netgrif.workflow.petrinet.domain.version.StringToVersionConverter;
import com.netgrif.workflow.petrinet.domain.version.Version;
import com.netgrif.workflow.petrinet.service.PetriNetService;
import com.netgrif.workflow.petrinet.web.responsebodies.PetriNetReference;
import com.netgrif.workflow.utils.FullPageRequest;
import com.netgrif.workflow.workflow.service.interfaces.IConfigurableMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConfigurableMenuService implements IConfigurableMenuService {

    @Autowired
    private PetriNetService petriNetService;
    @Autowired
    private StringToVersionConverter converter;

    /**
     * Constructs a map that can be used as a value for any {@link com.netgrif.workflow.petrinet.domain.dataset.MapOptionsField}.
     *
     * The map will contain strings related to process nets authored by the provided user.
     *
     * A key of the map is a string of the form "&lt;net identifier&gt;:&lt;net version&gt;".
     * The version portion of the string uses the dash (-) character to separate the major, minor a patch version numbers instead of the traditional dot character.
     *
     * A value of the map is an {@link I18nString} with no translations of the form  "&lt;net identifier&gt; : &lt;net version&gt;".
     * The default value of the net title is used.
     *
     * @param author currently logged user
     * @return an options map containing the identifiers and version of nets authored by the provided user as keys and their titles and versions as values
     */
    @Override
    public Map<String, I18nString> getNetsByAuthorAsMapOptions(User author, Locale locale){
        LoggedUser loggedAuthor = author.transformToLoggedUser();
        Map<String, Object> requestQuery = new HashMap<>();
        requestQuery.put("author.email", author.getEmail());
        List<PetriNetReference> nets = this.petriNetService.search(requestQuery, loggedAuthor, new FullPageRequest(), locale).getContent();

        Map<String, I18nString> options = new HashMap<>();

        for(PetriNetReference net : nets){
            String[] versionSplit = net.getVersion().split("\\.");
            I18nString titleAndVersion = new I18nString(net.getTitle() + " :" + net.getVersion());
            options.put(net.getIdentifier() + ":" + versionSplit[0] + "-" + versionSplit[1] + "-" + versionSplit[2], titleAndVersion);
        }

        return options;
    }

    /**
     * Constructs a map that can be used as a value for any {@link com.netgrif.workflow.petrinet.domain.dataset.MapOptionsField}.
     *
     * The map will contain roles from the net selected in the provided field, that are not present in either of the provided multichoice fields.
     *
     * A key of the map is a string of the form "&lt;role identifier&gt;:&lt;net identifier&gt;"
     *
     * A value of the map is the role title.
     *
     * @param processField the value of the field determines the process whose roles are put into the result.
     *                    The options key must match the format generated by the {@link ConfigurableMenuService#getNetsByAuthorAsMapOptions(User, Locale)} method
     * @param permittedRoles the roles selected in this multichoice will not be present in the result. The option keys of this multichoice must match the format returned by this method
     * @param bannedRoles the roles selected in this multichoice will not be present in the result. The option keys of this multichoice must match the format returned by this method
     * @return an options map containing the role and net identifiers as keys and the role titles as values
     */
    @Override
    public Map<String, I18nString> getAvailableRolesFromNet (EnumerationMapField processField, MultichoiceMapField permittedRoles, MultichoiceMapField bannedRoles) {

        String netImportId = processField.getValue().split(":")[0];
        String versionString = processField.getValue().split(":")[1].replace("-", ".");
        Version version = converter.convert(versionString);
        PetriNet net = petriNetService.getPetriNet(netImportId, version);

        return net.getRoles().values().stream()
                .filter(role -> (!permittedRoles.getOptions().containsKey(role.getImportId() + ":" + netImportId)
                && !bannedRoles.getOptions().containsKey(role.getImportId() + ":" + netImportId)))
                .collect(Collectors.toMap(o -> o.getImportId() + ":" + netImportId,  v -> new I18nString(v.getName())));
    }

    /**
     * Constructs a map that can be used as a value for any {@link com.netgrif.workflow.petrinet.domain.dataset.MapOptionsField}.
     *
     * The map will contain all the options from the input field except for those that are selected in the input field.
     *
     * @param mapField a map field whose value complement we want to get
     * @return a map containing all the deselected options of the provided field
     */
    @Override
    public Map<String, I18nString> removeSelectedRoles(MultichoiceMapField mapField) {

        Map<String, I18nString> updatedRoles = new LinkedHashMap<>(mapField.getOptions());
        updatedRoles.keySet().removeAll(mapField.getValue());
        return updatedRoles;
    }

    /**
     * Constructs a map that can be used as a value for any {@link com.netgrif.workflow.petrinet.domain.dataset.MapOptionsField}.
     *
     * The map will contain a union of the options that are already present in the {@code addedRoles} field with the options selected in the {@code rolesAvailable} field.
     *
     * The keys remain unchanged.
     *
     * The values of the map are a combination of the options from the {@code addedRoles} field (they remain unchanged)
     * and new values corresponding to the new keys from the {@code rolesAvailable} field. The new values are of the form "&lt;original value&gt; (&lt;net title&gt;)"
     *
     * @param addedRoles a field containing the preexisting options. The options of this field are assumed to be generated by this method.
     * @param processField a field containing the information about the selected process. The options of this field are assumed to be generated by the {@link ConfigurableMenuService#getNetsByAuthorAsMapOptions(User, Locale)} method
     * @param rolesAvailable a field containing the selection of the new roles. The options of this field are assumed to be generated by the {@link ConfigurableMenuService#getAvailableRolesFromNet(EnumerationMapField, MultichoiceMapField, MultichoiceMapField)} method
     * @return a map containing a quasi-union of the options from the {@code addedRoles} and {@code rolesAvailable} fields
     */
    @Override
    public Map<String, I18nString> addSelectedRoles(MultichoiceMapField addedRoles, EnumerationMapField processField, MultichoiceMapField rolesAvailable) {

        String netName = " (" + processField.getOptions().get(processField.getValue()).toString().split(":")[0] + ")";
        Map<String, I18nString> updatedRoles = new LinkedHashMap<>(addedRoles.getOptions());

        updatedRoles.putAll(rolesAvailable.getValue().stream()
                .collect(Collectors.toMap(x -> x, v -> new I18nString(rolesAvailable.getOptions().get(v).toString() + netName))));

        return updatedRoles;
    }
}