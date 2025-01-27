# UserList and UsersRef

## UserList

UserList is a type of data field. Values of this field represent ID of users in the system. Some basic information about
this field:

- this field currently does not have frontend representation
- this field does not have any init value, because when the process is created or imported, the given user may not exist
- we can add values to this field via actions
- it can serve as definition of permissions for volume of users

```xml
<data type="userList">
  <id>userList1</id>
  <title/>
</data>
```

## UsersRef

It is a new property of process and transition in PetriNet. It serves as a roleRef with a difference from it: the
content of the userList can be changed at runtime.

- usersRef references userList defined with its ID
- we define permissions for usersRef in a same way as for roleRef

```xml
<document>
  ...
  <data type="userList">
    <id>userList1</id>
    <title/>
  </data>
  ...
  <usersRef>
    <id>userList1</id>
    <caseLogic>
      <view>true</view>
      <delete>true</delete>
    </caseLogic>
  </usersRef>
  ...
  <transition>
    <id>1</id>
    <usersRef>
      <id>userList1</id>
      <logic>
        <perform>true</perform>
      </logic>
    </usersRef>
  </transition>
</document>
```

## Setting permissions

If we want to define permission only for a set of users, and we want to change the content of this set runtime, the
userList-usersRef combo is the best way to do so. You have to follow these steps:

1. Define new data field of type **userList** - required attribute is only the *id* of field.
2. For case permissions, define **usersRef** in *document* tag, for task permission define it in the corresponding *
   transition* tag
3. Into *logic* property of usersRef we define the permissions with boolean values - true means enable, false mean
   disable the given permission for user. The permissions can be the following:
    1. for cases:
        1. *view* - enable or disable the displaying of cases
        2. *delete* - enable or disable the deletion of cases
    2. for tasks:
        1. *perform* - enable or disable all the permissions
        2. *delegate* - enable or disable delegating tasks
        3. *assign* - enable or disable assign of tasks
        4. *cancel* - enable or disable canceling of tasks
        5. *finish* - enable or disable finish of tasks

[Permission resolution](roles/permissions.md?id=permissions)

[UsersRef Petri Net](../_media/roles/usersRef_functions.groovy)

[UsersRef Functions](../_media/roles/usersRef_net.xml)