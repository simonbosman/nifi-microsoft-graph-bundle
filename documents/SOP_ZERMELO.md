## SOP rooster synchronisation Zermelo

### Copy existing processor group
* Follow the CONNECT-TO-AKS sop for creating a connection to AKS and Apache NiFi.
* Open a browser and got the url [http://localhost:8080/nifi](http://localhost:8080/nifi)  
 
 ![Login Apache NiFi cluster](Login_Apache_NiFi.png)  

* Choose the DIS_Live Process Group
 Choose a Process Group for example Mondia scholengroep and make a copy.  

 ![Copy process group](Make_Copy.png)

### Create parameter context
* Paste the Process Group and right click and choose configure.  
 
 ![Click configure](Click_Configure.png)

* Choose a process group name and make a new parameter context.  

 ![Choose new parameter context](New_Parameter_Context.png)

* Give the parameter context a name and fill the following name value parameters.

 | Name | Value |
| :---- | :----- |
| zermelo.appointments.full | /api/v3/appointments/?startWeekOffset=0&endWeekOffset=3&fields=id,valid,appointmentInstance,start,end,startTimeSlotName,endTimeSlotName,subjects,groups,locations,teachers,cancelled,changeDescription,schedulerRemark,content,expectedStudentCount,expectedStudentCountOnline |    
| zermelo.appointments.quick | /api/v3/appointments/?startWeekOffset=0&endWeekOffset=3&modifiedSince=${now():toNumber():divide(1000):minus(900)}&fields=id,valid,appointmentInstance,start,end,startTimeSlotName,endTimeSlotName,subjects,groups,locations,teachers,cancelled,changeDescription,schedulerRemark,content,expectedStudentCount,expectedStudentCountOnline | 
| zermelo.avroschema | { "namespace": "nifi", "name": "appointment", "type": "record", "fields": [ { "name": "id", "type": "long" }, { "name": "appointmentInstance", "type": "long" }, { "name": "start", "type": "long", "logicalType":"timestamp-millis" }, { "name": "startDateTime", "type": ["null", "string"] }, { "name":"startDateIso","type":["null","string"] }, { "name": "end", "type": "long","logicalType":"timestamp-millis" }, { "name": "endDateTime", "type": ["null", "string" ]}, { "name": "groups", "type": {"type": "array", "items": "string", "default": "[]" }}, { "name": "locations", "type": {"type": "array", "items": "string", "default": "[]" }}, { "name": "valid", "type": "boolean" }, { "name": "cancelled", "type": "boolean" }, { "name": "changeDescription", "type": ["null", "string"] }, {"name": "schedulerRemark", "type": ["null", "string"] }, {"name": "expectedStudentCount", "type": "int" }, {"name": "expectedStudentCountOnline", "type": "int" }, {"name": "content", "type":["null", "string"] }, {"name": "startTimeSlotName", "type": "string" }, {"name": "endTimeSlotName", "type": "string" }, {"name": "email", "type": "string" }, {"name": "subjects", "type": {"type": "array", "items": "string", "default":"[]" }}, {"name": "teachers", "type": {"type": "array", "items": "string", "default":"[]"} } ] } |
| zermelo.baseurl | https://customer.zportal.nl | 
| zermelo.domain | mondia.nl | 
| zermelo.oauthtokenBearer | Bearer dlvhevtfk8og4hnjun02g14id8 |
| zermelo.teachers | /api/v3/users?isEmployee=true&fields=code,email |

### Configure controller services
* The processors utilized are dependent on various Apache NiFi controller services, including custom-made controller services for SPEYK. These service controllers must be enabled, started, and configured properly.  

* Select the MicrosoftGraphCredentialControllerService, set up the necessary properties, and enable it.

 ![Configure controller services](Configure_Controllers.png)

 | Property | Value |
 | :--------| :-----|
 | Auth Grant Type | client_credentials |
 | Auth Client id | 014b1961-2082-428a-940d-b5129e6ae9fe |
 | Auth Client Secret | qU-8Q~POFUZbTmbRb0MGC-XqEvFSlAMMaNoiHdAa |
 | Auth Tenant Id | 617f0231-3e08-4ebe-a403-3731ed7b9712 |
 | Auth Scope | https://graph.microsoft.com/.default |

* Enable the controller serivces:
 - JsonTreeReaderSchemaless
 - JsonTreeReader
 - JsonRecordSetWriter
 - ExternalHazelcastCacheManager

* Select the HazelcastMapCacheClient, configure its properties, and ensure it is enabled.

 ![Configure hazelcast](Hazelcast.png)
 
 | Propery | Value |
 | :-------| :---- |
 | Hazelcast Cache Manager | ExternalHazelcastCacheManager |
 | Hazelcast Cache Name | customer_scholengroep |
 | Hazelcast Entry Lifetime | 22 days |
 
 * Enable the controller service DistributedMapCacheLookupService

 ### Configure the created processor group 
 
 * Open the just create processor groep en right click on Teachers and choose Start

  ![Start processor group Teachers](Start_Teachers.png)
  
 *  Navigate to the processor group named "Appointments" and find the processor "InvokeRestApiZermeloFull." Double-click to open it.
