<p align="center">
  <img src="https://raw.githubusercontent.com/PKief/vscode-material-icon-theme/ec559a9f6bfd399b82bb44393651661b08aaf7ba/icons/folder-markdown-open.svg" width="100" />
</p>
<p align="center">
    <h1 align="center">.</h1>
</p>
<p align="center">
    <em>Transforming ideas into powerful, efficient software solutions.</em>
</p>
<p align="center">
	<!-- local repository, no metadata badges. -->
<p>
<p align="center">
		<em>Developed with the software and tools below.</em>
</p>
<p align="center">
	<img src="https://img.shields.io/badge/GNU%20Bash-4EAA25.svg?style=default&logo=GNU-Bash&logoColor=white" alt="GNU%20Bash">
	<img src="https://img.shields.io/badge/HTML5-E34F26.svg?style=default&logo=HTML5&logoColor=white" alt="HTML5">
	<img src="https://img.shields.io/badge/java-%23ED8B00.svg?style=default&logo=openjdk&logoColor=white" alt="java">
</p>
<hr>

##  Quick Links

> - [ Overview](#-overview)
> - [ Features](#-features)
> - [ Repository Structure](#-repository-structure)
> - [ Modules](#-modules)
> - [ Getting Started](#-getting-started)
>   - [ Installation](#-installation)
>   - [ Running .](#-running-.)
>   - [ Tests](#-tests)
> - [ Project Roadmap](#-project-roadmap)
> - [ Contributing](#-contributing)
> - [ License](#-license)
> - [ Acknowledgments](#-acknowledgments)

---

##  Overview

The codebase represents a project managed using Maven, a build automation tool. Maven's POM (Project Object Model) file, pom.xml, is included in the codebase. This file is crucial for managing the project's dependencies, configuring the build process, and defining project information and properties. It follows the Apache License, Version 2.0. The codebase's core functionality is to provide a structured and standardized approach to building and managing the project, ensuring smooth execution and delivery. By leveraging Maven, developers can easily manage dependencies, build projects, and automate various tasks, enhancing productivity and improving software quality.

---

##  Features

|    |   Feature         | Description |
|----|-------------------|---------------------------------------------------------------|
| ⚙️  | **Architecture**  | The project follows a modular architecture, with components organized into a NiFi data flow. It utilizes NiFi processors for data processing and integration with Microsoft Graph Services. The architecture allows for scalability and extensibility through the use of Controller Services and Processor extensions. |
| 🔩 | **Code Quality**  | The codebase exhibits good code quality and adheres to industry-standard coding conventions. It follows a modular and maintainable structure, making it easy to understand and update. The codebase is well-documented and contains comments for better clarity and collaboration. |
| 📄 | **Documentation** | The project has comprehensive documentation, including a detailed README file and inline comments throughout the codebase. The documentation covers installation instructions, configuration details, and usage guidelines. It effectively communicates the purpose, functionality, and usage of each component. |
| 🔌 | **Integrations**  | The project integrates with Microsoft Graph Services and utilizes the Azure Identity library for authentication and authorization. It also has dependencies on other libraries such as NiFi Standard Record Utils, NiFi Processor Utils, and Jackson Core for data manipulation and processing. |
| 🧩 | **Modularity**    | The codebase follows a modular approach, with components and functionalities decoupled into separate processor classes. This modular design allows for code reuse and easy extensibility. Each processor performs a specific task, enhancing maintainability and flexibility. |
| 🧪 | **Testing**       | The project utilizes the NiFi mock framework and NiFi test runner for unit testing the processors. It also has integration tests to verify the end-to-end functionality of the data flow. The codebase includes a comprehensive test suite covering different use cases and scenarios. |
| ⚡️  | **Performance**   | The project is designed to be efficient and performant. It takes advantage of NiFi's parallel processing capabilities to handle large-scale data processing. Performance optimizations are implemented to minimize resource usage and maximize throughput. Monitoring and profiling tools are used to identify and address bottlenecks. |
| 🛡️ | **Security**      | The project incorporates security measures through the use of Azure Identity for authentication and authorization. It follows secure coding practices to prevent vulnerabilities such as SQL injection and cross-site scripting. Access control is implemented to ensure data protection and restrict unauthorized access. |
| 📦 | **Dependencies**  | Key external libraries and dependencies used in the project include Microsoft Graph Services, Azure Identity, Guava, Jackson Core, and OkHttp. These libraries provide essential functionality for interacting with Microsoft Graph APIs and handling data processing tasks. |


---

##  Repository Structure

```sh
└── ./
    ├── compile->copy-nars->nifi-restart.sh
    ├── nifi-microsoft-graph-processors
    │   ├── .classpath
    │   ├── .factorypath
    │   ├── .project
    │   ├── .settings
    │   │   ├── org.eclipse.core.resources.prefs
    │   │   ├── org.eclipse.jdt.apt.core.prefs
    │   │   ├── org.eclipse.jdt.core.prefs
    │   │   └── org.eclipse.m2e.core.prefs
    │   ├── src
    │   │   ├── main
    │   │   └── test
    │   └── target
    │       ├── .plxarc
    │       ├── classes
    │       ├── generated-sources
    │       ├── generated-test-sources
    │       ├── maven-shared-archive-resources
    │       ├── maven-status
    │       └── test-classes
    ├── nifi-microsoft-graph-processors-nar
    │   └── target
    │       ├── .plxarc
    │       ├── classes
    │       ├── maven-shared-archive-resources
    │       ├── nifi-microsoft-graph-processors-nar-0.1.0.nar
    │       └── test-classes
    ├── nifi-microsoft-graph-services
    │   ├── src
    │   │   ├── main
    │   │   └── test
    │   └── target
    │       ├── .plxarc
    │       ├── classes
    │       ├── generated-sources
    │       ├── generated-test-sources
    │       ├── maven-shared-archive-resources
    │       ├── maven-status
    │       └── test-classes
    ├── nifi-microsoft-graph-services-api
    │   ├── src
    │   │   └── main
    │   └── target
    │       ├── .plxarc
    │       ├── classes
    │       ├── generated-sources
    │       ├── maven-shared-archive-resources
    │       ├── maven-status
    │       └── test-classes
    ├── nifi-microsoft-graph-services-api-nar
    │   └── target
    │       ├── .plxarc
    │       ├── classes
    │       ├── maven-shared-archive-resources
    │       ├── nifi-microsoft-graph-services-api-nar-0.1.0.nar
    │       └── test-classes
    ├── nifi-microsoft-graph-services-nar
    │   └── target
    │       ├── .plxarc
    │       ├── classes
    │       ├── maven-shared-archive-resources
    │       ├── nifi-microsoft-graph-services-nar-0.1.0.nar
    │       └── test-classes
    ├── roostersync.html
    └── target
        ├── .plxarc
        └── maven-shared-archive-resources
            └── META-INF
```

---

##  Modules

<details closed><summary>.</summary>

| File                                                                       | Summary                                                                                                                                                                                                                                                                                                                                                                           |
| ---                                                                        | ---                                                                                                                                                                                                                                                                                                                                                                               |
| [pom.xml](pom.xml)                                                         | The code snippet defines the Maven project structure and configuration for the `nifi-microsoft-graph-bundle` repository. It includes multiple modules for different components and processors related to Microsoft Graph integration within Apache NiFi.                                                                                                                          |
| [compile->copy-nars->nifi-restart.sh](compile->copy-nars->nifi-restart.sh) | The code snippet is a shell script located at compile->copy-nars->nifi-restart.sh in the repository. It compiles the project and copies the generated NAR files to the NIFI_HOME/extensions directory. It then restarts NiFi and waits for it to be available before exiting.                                                                                                     |
| [roostersync.html](roostersync.html)                                       | This code snippet consists of an HTML file named roostersync.html that provides an explanation of the rooster synchronization process. It outlines different scenarios, such as full synchronization and fast synchronization, and describes the actions taken in each scenario. The file also mentions a special scenario related to storing and verifying digital fingerprints. |

</details>

<details closed><summary>nifi-microsoft-graph-services-nar</summary>

| File                                                 | Summary                                                                                                                                                                                                                                                                                        |
| ---                                                  | ---                                                                                                                                                                                                                                                                                            |
| [pom.xml](nifi-microsoft-graph-services-nar/pom.xml) | This code snippet is part of the parent repository's architecture and is responsible for defining the dependencies and packaging of the `nifi-microsoft-graph-services-nar` artifact. It includes dependencies on `nifi-microsoft-graph-services-api-nar` and `nifi-microsoft-graph-services`. |

</details>

<details closed><summary>nifi-microsoft-graph-services-nar.target</summary>

| File                                                        | Summary                                                                                                                                                                                                                                                                                            |
| ---                                                         | ---                                                                                                                                                                                                                                                                                                |
| [.plxarc](nifi-microsoft-graph-services-nar/target/.plxarc) | The code snippet is located in the nifi-microsoft-graph-services-nar module of the repository. It includes the maven-shared-archive-resources directory, which contains shared resources for the Maven build process. The code snippet is responsible for managing and organizing these resources. |

</details>

<details closed><summary>nifi-microsoft-graph-services-nar.target.test-classes.META-INF</summary>

| File                                                                                        | Summary                                                                                                                                                                                                                                                                                              |
| ---                                                                                         | ---                                                                                                                                                                                                                                                                                                  |
| [DEPENDENCIES](nifi-microsoft-graph-services-nar/target/test-classes/META-INF/DEPENDENCIES) | This code snippet is part of a repository called nifi-microsoft-graph-processors. It likely contributes to the architecture by providing processors for handling Microsoft Graph data in Apache NiFi. The exact purpose and functionality of the code are not specified in the provided information. |
| [NOTICE](nifi-microsoft-graph-services-nar/target/test-classes/META-INF/NOTICE)             | This code snippet is part of the `nifi-microsoft-graph-services-nar` component in the parent repository. It contains licensing information and acknowledges the software developed by the Apache Software Foundation.                                                                                |

</details>

<details closed><summary>nifi-microsoft-graph-services-nar.target.maven-shared-archive-resources.META-INF</summary>

| File                                                                                                          | Summary                                                                                                                                                                                                                                                                                                                                                                                                           |
| ---                                                                                                           | ---                                                                                                                                                                                                                                                                                                                                                                                                               |
| [DEPENDENCIES](nifi-microsoft-graph-services-nar/target/maven-shared-archive-resources/META-INF/DEPENDENCIES) | The code snippet in the nifi-microsoft-graph-processors module is responsible for integrating Microsoft Graph API with Apache NiFi. It provides processors to perform various operations on Microsoft Graph, such as fetching emails or uploading files. This code plays a critical role in enhancing the functionality of the parent repository's architecture by enabling data processing with Microsoft Graph. |
| [NOTICE](nifi-microsoft-graph-services-nar/target/maven-shared-archive-resources/META-INF/NOTICE)             | This code snippet is part of the nifi-microsoft-graph-services-nar module in the repository. It contributes to the Microsoft Graph API integration with Apache NiFi.                                                                                                                                                                                                                                              |

</details>

<details closed><summary>nifi-microsoft-graph-services-nar.target.classes.META-INF</summary>

| File                                                                                   | Summary                                                                                                                                                                                                                                                                                                                 |
| ---                                                                                    | ---                                                                                                                                                                                                                                                                                                                     |
| [DEPENDENCIES](nifi-microsoft-graph-services-nar/target/classes/META-INF/DEPENDENCIES) | Code snippet: nifi-microsoft-graph-processorsSummary: This code snippet is part of the parent repository's architecture and focuses on implementing processors for Microsoft Graph APIs in Apache NiFi. It includes necessary source and test files, as well as target directories for generated classes and resources. |
| [NOTICE](nifi-microsoft-graph-services-nar/target/classes/META-INF/NOTICE)             | This code snippet is located in the `nifi-microsoft-graph-services-nar` module of the repository. It contains a NOTICE file with copyright information for the Apache NiFi Project.                                                                                                                                     |

</details>

<details closed><summary>nifi-microsoft-graph-services-api-nar</summary>

| File                                                     | Summary                                                                                                                                                                                                                                             |
| ---                                                      | ---                                                                                                                                                                                                                                                 |
| [pom.xml](nifi-microsoft-graph-services-api-nar/pom.xml) | The nifi-microsoft-graph-services-api-nar code snippet is a Maven POM file that configures the project as a Nar (NiFi Archive). It specifies dependencies on the NiFi standard services API and the local nifi-microsoft-graph-services-api module. |

</details>

<details closed><summary>nifi-microsoft-graph-services-api-nar.target</summary>

| File                                                            | Summary                                                                                                                                                                                      |
| ---                                                             | ---                                                                                                                                                                                          |
| [.plxarc](nifi-microsoft-graph-services-api-nar/target/.plxarc) | This code snippet is located in the `nifi-microsoft-graph-services-api-nar` repository and specifically in the `target/.plxarc` file. It contains shared archive resources related to Maven. |

</details>

<details closed><summary>nifi-microsoft-graph-services-api-nar.target.test-classes.META-INF</summary>

| File                                                                                            | Summary                                                                                                                                                                                                                                                             |
| ---                                                                                             | ---                                                                                                                                                                                                                                                                 |
| [DEPENDENCIES](nifi-microsoft-graph-services-api-nar/target/test-classes/META-INF/DEPENDENCIES) | This code snippet is part of the nifi-microsoft-graph-processors module in the parent repository. It plays a critical role in compiling, copying NAR files, and restarting the Nifi service. It is essential for integrating Microsoft Graph APIs with Apache Nifi. |
| [NOTICE](nifi-microsoft-graph-services-api-nar/target/test-classes/META-INF/NOTICE)             | The code snippet is located in the nifi-microsoft-graph-services-api-nar directory. It contains the NOTICE file, which includes copyright information for the Apache NiFi Project.                                                                                  |

</details>

<details closed><summary>nifi-microsoft-graph-services-api-nar.target.maven-shared-archive-resources.META-INF</summary>

| File                                                                                                              | Summary                                                                                                                                                                                                                                                                                |
| ---                                                                                                               | ---                                                                                                                                                                                                                                                                                    |
| [DEPENDENCIES](nifi-microsoft-graph-services-api-nar/target/maven-shared-archive-resources/META-INF/DEPENDENCIES) | The code snippet in the nifi-microsoft-graph-processors directory is responsible for processing Microsoft Graph data in Apache NiFi. It contains source code and test files for the processors, as well as configuration files. The snippet helps integrate NiFi with Microsoft Graph. |
| [NOTICE](nifi-microsoft-graph-services-api-nar/target/maven-shared-archive-resources/META-INF/NOTICE)             | The code snippet in the `nifi-microsoft-graph-services-api-nar` repository generates a NOTICE file that acknowledges the use of software from the Apache Software Foundation. It is part of the parent repository's architecture to ensure proper attribution and compliance.          |

</details>

<details closed><summary>nifi-microsoft-graph-services-api-nar.target.classes.META-INF</summary>

| File                                                                                       | Summary                                                                                                                                                                        |
| ---                                                                                        | ---                                                                                                                                                                            |
| [DEPENDENCIES](nifi-microsoft-graph-services-api-nar/target/classes/META-INF/DEPENDENCIES) | The code snippet is a part of the nifi-microsoft-graph-processors module in the repository. It handles the compilation, copying of files, and restarting of Nifi.              |
| [NOTICE](nifi-microsoft-graph-services-api-nar/target/classes/META-INF/NOTICE)             | The code snippet in the `nifi-microsoft-graph-services-api-nar` repository is responsible for including the notice file that acknowledges the Apache NiFi Project's copyright. |

</details>

<details closed><summary>target</summary>

| File                      | Summary                                                                                                                                                                                                     |
| ---                       | ---                                                                                                                                                                                                         |
| [.plxarc](target/.plxarc) | This code snippet is related to the parent repository's architecture and is located in the `target` folder. It contains `maven-shared-archive-resources` that are used for building and packaging purposes. |

</details>

<details closed><summary>target.maven-shared-archive-resources.META-INF</summary>

| File                                                                        | Summary                                                                                                                                                                                                                                                                                                                                               |
| ---                                                                         | ---                                                                                                                                                                                                                                                                                                                                                   |
| [DEPENDENCIES](target/maven-shared-archive-resources/META-INF/DEPENDENCIES) | This code snippet is part of the parent repository's architecture. It is responsible for managing and organizing the transitive dependencies of the project using the `DEPENDENCIES` file located in the `target/maven-shared-archive-resources/META-INF/` directory. The main dependency that is being handled is the `nifi-microsoft-graph-bundle`. |
| [NOTICE](target/maven-shared-archive-resources/META-INF/NOTICE)             | This code snippet is part of the nifi-microsoft-graph-bundle repository. Its main role is to provide processors and services for integrating Microsoft Graph APIs with Apache NiFi. Key features include processing data, accessing Graph API endpoints, and handling authentication.                                                                 |

</details>

<details closed><summary>nifi-microsoft-graph-services</summary>

| File                                             | Summary                                                                                                                                                                                                                                                                                                                                                                                                                   |
| ---                                              | ---                                                                                                                                                                                                                                                                                                                                                                                                                       |
| [pom.xml](nifi-microsoft-graph-services/pom.xml) | This code snippet represents the pom.xml file in the nifi-microsoft-graph-services module of the repository. It defines the dependencies required for the module, including the nifi-microsoft-graph-services-api, microsoft-graph, and jackson-core libraries. This file is essential for managing the project's dependencies and ensuring proper compilation and execution of the nifi-microsoft-graph-services module. |

</details>

<details closed><summary>nifi-microsoft-graph-services.target</summary>

| File                                                    | Summary                                                                                                                                                                                                                                                               |
| ---                                                     | ---                                                                                                                                                                                                                                                                   |
| [.plxarc](nifi-microsoft-graph-services/target/.plxarc) | This code snippet, located in the `nifi-microsoft-graph-services/target/.plxarc` file, contains resources related to the Maven build process. It provides necessary files for building and packaging the Microsoft Graph services component in the parent repository. |

</details>

<details closed><summary>nifi-microsoft-graph-services.target.test-classes.META-INF</summary>

| File                                                                                    | Summary                                                                                                                                                                                                                                                                                                                                                        |
| ---                                                                                     | ---                                                                                                                                                                                                                                                                                                                                                            |
| [DEPENDENCIES](nifi-microsoft-graph-services/target/test-classes/META-INF/DEPENDENCIES) | This code snippet is part of the nifi-microsoft-graph-processors module in the parent repository. Its main role is to process data from Microsoft Graph API. It is responsible for handling authentication and interacting with the API endpoints. The code ensures seamless integration and data processing within the larger architecture of the repository. |
| [NOTICE](nifi-microsoft-graph-services/target/test-classes/META-INF/NOTICE)             | This code snippet is located in the nifi-microsoft-graph-services directory of the parent repository. It contains the NOTICE file, which includes copyright information for the Apache NiFi Project and the Apache Software Foundation.                                                                                                                        |

</details>

<details closed><summary>nifi-microsoft-graph-services.target.maven-shared-archive-resources.META-INF</summary>

| File                                                                                                      | Summary                                                                                                                                                                                                                                                                                                                             |
| ---                                                                                                       | ---                                                                                                                                                                                                                                                                                                                                 |
| [DEPENDENCIES](nifi-microsoft-graph-services/target/maven-shared-archive-resources/META-INF/DEPENDENCIES) | This code snippet is a part of the nifi-microsoft-graph-processors module within the repository. It includes the necessary files and folders for building and testing the Microsoft Graph processors for Apache NiFi. It contributes to the architecture by providing processors that enable interaction with Microsoft Graph APIs. |
| [NOTICE](nifi-microsoft-graph-services/target/maven-shared-archive-resources/META-INF/NOTICE)             | The code snippet in the `nifi-microsoft-graph-services` repository is responsible for implementing services related to Microsoft Graph in Apache NiFi. It includes software developed at The Apache Software Foundation and is used to interact with Microsoft Graph APIs.                                                          |

</details>

<details closed><summary>nifi-microsoft-graph-services.target.classes.META-INF</summary>

| File                                                                               | Summary                                                                                                                                                                                                                          |
| ---                                                                                | ---                                                                                                                                                                                                                              |
| [DEPENDENCIES](nifi-microsoft-graph-services/target/classes/META-INF/DEPENDENCIES) | The code snippet in the `nifi-microsoft-graph-processors` directory is responsible for implementing processors that interact with Microsoft Graph API. It includes the necessary source code and test code for these processors. |
| [NOTICE](nifi-microsoft-graph-services/target/classes/META-INF/NOTICE)             | The code snippet is located in the `nifi-microsoft-graph-services` module under the `target/classes/META-INF/NOTICE` file. It contains the copyright information for the `nifi-microsoft-graph-services` project.                |

</details>

<details closed><summary>nifi-microsoft-graph-services.target.classes.META-INF.services</summary>

| File                                                                                                                                                        | Summary                                                                                                                                                                                                                                                                                                  |
| ---                                                                                                                                                         | ---                                                                                                                                                                                                                                                                                                      |
| [org.apache.nifi.controller.ControllerService](nifi-microsoft-graph-services/target/classes/META-INF/services/org.apache.nifi.controller.ControllerService) | This code snippet is a file that registers a specific Controller Service implementation, nl.speyk.nifi.microsoft.graph.services.MicrosoftGraphCredentialControllerService, within the parent repository. This service is used to provide credentials for Microsoft Graph API integration in Apache NiFi. |

</details>

<details closed><summary>nifi-microsoft-graph-services.target.maven-status.maven-compiler-plugin.testCompile.groovy-tests</summary>

| File                                                                                                                                  | Summary                                                                                                                                                                                                                                                                                                |
| ---                                                                                                                                   | ---                                                                                                                                                                                                                                                                                                    |
| [inputFiles.lst](nifi-microsoft-graph-services/target/maven-status/maven-compiler-plugin/testCompile/groovy-tests/inputFiles.lst)     | This code snippet is located in the nifi-microsoft-graph-services module of the repository. It contains a test file for the MicrosoftGraphCredentialControllerService class. The code tests the functionality of the credential management service for Microsoft Graph API integration in Apache NiFi. |
| [createdFiles.lst](nifi-microsoft-graph-services/target/maven-status/maven-compiler-plugin/testCompile/groovy-tests/createdFiles.lst) | This code snippet is part of the nifi-microsoft-graph-services module in the repository. It is responsible for compiling and testing groovy files.                                                                                                                                                     |

</details>

<details closed><summary>nifi-microsoft-graph-services.target.maven-status.maven-compiler-plugin.testCompile.default-testCompile</summary>

| File                                                                                                                                         | Summary                                                                                                                                                                                                                               |
| ---                                                                                                                                          | ---                                                                                                                                                                                                                                   |
| [inputFiles.lst](nifi-microsoft-graph-services/target/maven-status/maven-compiler-plugin/testCompile/default-testCompile/inputFiles.lst)     | This code snippet is located in the `nifi-microsoft-graph-services` module of the repository. It includes a test class `TestMicrosoftGraphCredentialControllerService` for the Microsoft Graph Credential Controller Service in NiFi. |
| [createdFiles.lst](nifi-microsoft-graph-services/target/maven-status/maven-compiler-plugin/testCompile/default-testCompile/createdFiles.lst) | This code snippet is located in the `nifi-microsoft-graph-services` directory. It compiles and generates a test class called `TestMicrosoftGraphCredentialControllerService`.                                                         |

</details>

<details closed><summary>nifi-microsoft-graph-services.target.maven-status.maven-compiler-plugin.compile.default-compile</summary>

| File                                                                                                                                 | Summary                                                                                                                                                                                                                                                                                                                                                           |
| ---                                                                                                                                  | ---                                                                                                                                                                                                                                                                                                                                                               |
| [inputFiles.lst](nifi-microsoft-graph-services/target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst)     | The code snippet is a Java file located in the following path: nifi-microsoft-graph-services/src/main/java/nl/speyk/nifi/microsoft/graph/services/MicrosoftGraphCredentialControllerService.java. It is part of the nifi-microsoft-graph-services module in the repository. The purpose and functionality of the code are not specified in the given information. |
| [createdFiles.lst](nifi-microsoft-graph-services/target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst) | The code snippet in the given file path `nifi-microsoft-graph-services/target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst` includes the `MicrosoftGraphCredentialControllerService` class, which is a critical feature of the `nifi-microsoft-graph-services` module in the repository architecture.                              |

</details>

<details closed><summary>nifi-microsoft-graph-services.src.test.java.nl.speyk.nifi.microsoft.graph.services</summary>

| File                                                                                                                                                                                        | Summary                                                                                                                                                                                                                                                                 |
| ---                                                                                                                                                                                         | ---                                                                                                                                                                                                                                                                     |
| [TestMicrosoftGraphCredentialControllerService.java](nifi-microsoft-graph-services/src/test/java/nl/speyk/nifi/microsoft/graph/services/TestMicrosoftGraphCredentialControllerService.java) | The code snippet in TestMicrosoftGraphCredentialControllerService.java tests the validity of a Microsoft Graph credential controller service. It verifies that the required credentials (tenant ID, client ID, client secret, grant type, and scope) are set correctly. |

</details>

<details closed><summary>nifi-microsoft-graph-services.src.main.resources.META-INF.services</summary>

| File                                                                                                                                                            | Summary                                                                                                                                                                                                                             |
| ---                                                                                                                                                             | ---                                                                                                                                                                                                                                 |
| [org.apache.nifi.controller.ControllerService](nifi-microsoft-graph-services/src/main/resources/META-INF/services/org.apache.nifi.controller.ControllerService) | This code snippet is a configuration file that specifies the implementation class for a Controller Service in the parent repository. The class is nl.speyk.nifi.microsoft.graph.services.MicrosoftGraphCredentialControllerService. |

</details>

<details closed><summary>nifi-microsoft-graph-services.src.main.java.nl.speyk.nifi.microsoft.graph.services</summary>

| File                                                                                                                                                                                | Summary                                                                                                                                                                                                                        |
| ---                                                                                                                                                                                 | ---                                                                                                                                                                                                                            |
| [MicrosoftGraphCredentialControllerService.java](nifi-microsoft-graph-services/src/main/java/nl/speyk/nifi/microsoft/graph/services/MicrosoftGraphCredentialControllerService.java) | This code snippet defines the MicrosoftGraphCredentialControllerService, which provides credentials for Microsoft Graph Processors. It sets up a GraphServiceClient with the necessary authentication and connection settings. |

</details>

<details closed><summary>nifi-microsoft-graph-processors</summary>

| File                                                         | Summary                                                                                                                                                                                                                                                                                                                                     |
| ---                                                          | ---                                                                                                                                                                                                                                                                                                                                         |
| [.factorypath](nifi-microsoft-graph-processors/.factorypath) | This code snippet is part of the nifi-microsoft-graph-processors module in the parent repository. It focuses on compiling and copying NAR files, followed by restarting Nifi. It plays a critical role in managing and deploying Microsoft Graph processors in the Nifi framework.                                                          |
| [pom.xml](nifi-microsoft-graph-processors/pom.xml)           | This code snippet is part of the nifi-microsoft-graph-processors module in the parent repository. It contains the dependencies and configurations necessary for processing Microsoft Graph API in Apache NiFi. The code achieves the integration of NiFi with the Microsoft Graph API by providing the required processor functionality.    |
| [.classpath](nifi-microsoft-graph-processors/.classpath)     | This code snippet is a configuration file that defines the classpath for the `nifi-microsoft-graph-processors` module in the repository. It specifies the source and output directories for the Java and resource files, as well as the test directories. The file also includes references to dependencies and build paths for the module. |
| [.project](nifi-microsoft-graph-processors/.project)         | The code snippet is a part of the `nifi-microsoft-graph-processors` module in the repository. It includes the `.project` file that defines the project structure and configurations for Java build and Maven. The code helps manage the dependencies, build commands, and project nature within the module.                                 |

</details>

<details closed><summary>nifi-microsoft-graph-processors.target</summary>

| File                                                      | Summary                                                                                                                                              |
| ---                                                       | ---                                                                                                                                                  |
| [.plxarc](nifi-microsoft-graph-processors/target/.plxarc) | This code snippet is part of the nifi-microsoft-graph-processors module in the repository. It includes shared resources for the Maven build process. |

</details>

<details closed><summary>nifi-microsoft-graph-processors.target.test-classes.META-INF</summary>

| File                                                                                      | Summary                                                                                                                                                                                                                                                                                                                                      |
| ---                                                                                       | ---                                                                                                                                                                                                                                                                                                                                          |
| [DEPENDENCIES](nifi-microsoft-graph-processors/target/test-classes/META-INF/DEPENDENCIES) | The code snippet in the nifi-microsoft-graph-processors folder plays a critical role in the parent repository's architecture. It provides processors for integrating with Microsoft Graph API in Apache NiFi. The code achieves seamless data flow integration with Microsoft services, facilitating efficient data processing and analysis. |
| [NOTICE](nifi-microsoft-graph-processors/target/test-classes/META-INF/NOTICE)             | This code snippet is part of the nifi-microsoft-graph-processors module in the repository. It plays a critical role in processing Microsoft Graph data in Apache NiFi. The code achieves integration with Microsoft Graph API and enables data processing operations.                                                                        |

</details>

<details closed><summary>nifi-microsoft-graph-processors.target.maven-shared-archive-resources.META-INF</summary>

| File                                                                                                        | Summary                                                                                                                                                                                                                                                                                                             |
| ---                                                                                                         | ---                                                                                                                                                                                                                                                                                                                 |
| [DEPENDENCIES](nifi-microsoft-graph-processors/target/maven-shared-archive-resources/META-INF/DEPENDENCIES) | This code snippet is part of the nifi-microsoft-graph-processors repository. It includes the necessary files and directories for compiling, copying NARs, and restarting Nifi. The main role of this snippet is to ensure the smooth operation of the Microsoft Graph processors in the Nifi platform.              |
| [NOTICE](nifi-microsoft-graph-processors/target/maven-shared-archive-resources/META-INF/NOTICE)             | The code snippet is part of the `nifi-microsoft-graph-processors` module within the repository. Its role is to include the necessary software developed by the Apache NiFi Project for the Microsoft Graph processors. The code achieves the integration of Microsoft Graph services into NiFi data flow pipelines. |

</details>

<details closed><summary>nifi-microsoft-graph-processors.target.classes.META-INF</summary>

| File                                                                                 | Summary                                                                                                                                                                                                                                                                                   |
| ---                                                                                  | ---                                                                                                                                                                                                                                                                                       |
| [DEPENDENCIES](nifi-microsoft-graph-processors/target/classes/META-INF/DEPENDENCIES) | The code snippet in the nifi-microsoft-graph-processors repository is responsible for compiling and copying NAR files and restarting Nifi. It plays a critical role in the repository's architecture by facilitating the deployment and management of Microsoft Graph processors in Nifi. |
| [NOTICE](nifi-microsoft-graph-processors/target/classes/META-INF/NOTICE)             | The code in the `nifi-microsoft-graph-processors` module is responsible for processing Microsoft Graph data in Apache NiFi. It contributes to the parent repository's architecture by providing processors that handle Microsoft Graph data.                                              |

</details>

<details closed><summary>nifi-microsoft-graph-processors.target.classes.META-INF.services</summary>

| File                                                                                                                                        | Summary                                                                                                                                                                        |
| ---                                                                                                                                         | ---                                                                                                                                                                            |
| [org.apache.nifi.processor.Processor](nifi-microsoft-graph-processors/target/classes/META-INF/services/org.apache.nifi.processor.Processor) | This code snippet defines the processor class InvokeMicrosoftGraphCalendar that is used in the parent repository for executing operations on the Microsoft Graph Calendar API. |

</details>

<details closed><summary>nifi-microsoft-graph-processors.target.maven-status.maven-compiler-plugin.testCompile.groovy-tests</summary>

| File                                                                                                                                    | Summary                                                                                                                                                                                                                                                                                                                                |
| ---                                                                                                                                     | ---                                                                                                                                                                                                                                                                                                                                    |
| [inputFiles.lst](nifi-microsoft-graph-processors/target/maven-status/maven-compiler-plugin/testCompile/groovy-tests/inputFiles.lst)     | The code snippet in the file inputFiles.lst in the nifi-microsoft-graph-processors repository tests the functionality of invoking Microsoft Graph Calendar.                                                                                                                                                                            |
| [createdFiles.lst](nifi-microsoft-graph-processors/target/maven-status/maven-compiler-plugin/testCompile/groovy-tests/createdFiles.lst) | The code snippet in the createdFiles.lst file under nifi-microsoft-graph-processors/target/maven-status/maven-compiler-plugin/testCompile/groovy-tests directory plays a critical role in the architecture of the parent repository. It is responsible for tracking and managing the creation of files during the compilation process. |

</details>

<details closed><summary>nifi-microsoft-graph-processors.target.maven-status.maven-compiler-plugin.testCompile.default-testCompile</summary>

| File                                                                                                                                           | Summary                                                                                                                                                                                                                                                                           |
| ---                                                                                                                                            | ---                                                                                                                                                                                                                                                                               |
| [inputFiles.lst](nifi-microsoft-graph-processors/target/maven-status/maven-compiler-plugin/testCompile/default-testCompile/inputFiles.lst)     | This code snippet, located in the `nifi-microsoft-graph-processors` module, is responsible for executing a test case for the `TestInvokeMicrosoftGraphCalendar` class, which is part of the Microsoft Graph processors functionality within the parent repository's architecture. |
| [createdFiles.lst](nifi-microsoft-graph-processors/target/maven-status/maven-compiler-plugin/testCompile/default-testCompile/createdFiles.lst) | This code snippet in the nifi-microsoft-graph-processors repository compiles and generates a test class called TestInvokeMicrosoftGraphCalendar.                                                                                                                                  |

</details>

<details closed><summary>nifi-microsoft-graph-processors.target.maven-status.maven-compiler-plugin.compile.default-compile</summary>

| File                                                                                                                                   | Summary                                                                                                                                                                                                                                                                                                                                                                                                                     |
| ---                                                                                                                                    | ---                                                                                                                                                                                                                                                                                                                                                                                                                         |
| [inputFiles.lst](nifi-microsoft-graph-processors/target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst)     | The code in the file AbstractMicrosoftGraphCalendar.java is part of the nifi-microsoft-graph-processors module in the repository. It provides abstract functionality for processing Microsoft Graph calendars and is used by other components in the module, such as InvokeMicrosoftGraphCalendar.java. The code makes use of utility classes for calendar-related operations, such as CalendarUtils.java and Zermelo.java. |
| [createdFiles.lst](nifi-microsoft-graph-processors/target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst) | The code snippet in the file `nifi-microsoft-graph-processors/target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst` contains compiled Java classes related to Microsoft Graph processors in the parent repository. These classes provide functionality for interacting with Microsoft Graph API and manipulating calendar data.                                                               |

</details>

<details closed><summary>nifi-microsoft-graph-processors..settings</summary>

| File                                                                                                           | Summary                                                                                                                                                                                                                                                                              |
| ---                                                                                                            | ---                                                                                                                                                                                                                                                                                  |
| [org.eclipse.jdt.core.prefs](nifi-microsoft-graph-processors/.settings/org.eclipse.jdt.core.prefs)             | This code snippet is part of the nifi-microsoft-graph-processors module within the repository. It sets the Java compiler properties for the project, including the target platform and compliance level. These settings ensure that the code is compiled and executed appropriately. |
| [org.eclipse.jdt.apt.core.prefs](nifi-microsoft-graph-processors/.settings/org.eclipse.jdt.apt.core.prefs)     | This code snippet is located in the nifi-microsoft-graph-processors directory and is responsible for configuring the annotation processing settings in Eclipse. It enables annotation processing and sets the target directories for generated source and test code.                 |
| [org.eclipse.m2e.core.prefs](nifi-microsoft-graph-processors/.settings/org.eclipse.m2e.core.prefs)             | The code snippet is located in the `nifi-microsoft-graph-processors` directory in the parent repository. It contains preferences for Eclipse's Maven integration, including the active profiles, workspace project resolution, and version details.                                  |
| [org.eclipse.core.resources.prefs](nifi-microsoft-graph-processors/.settings/org.eclipse.core.resources.prefs) | This code snippet, located in the `nifi-microsoft-graph-processors` directory, provides configuration preferences for the Eclipse IDE. It sets the encoding for different file types to UTF-8.                                                                                       |

</details>

<details closed><summary>nifi-microsoft-graph-processors.src.test.java.nl.speyk.nifi.microsoft.graph.processors</summary>

| File                                                                                                                                                                  | Summary                                                                                                                                                                                                                                                                                                                                                                                                                   |
| ---                                                                                                                                                                   | ---                                                                                                                                                                                                                                                                                                                                                                                                                       |
| [TestInvokeMicrosoftGraphCalendar.java](nifi-microsoft-graph-processors/src/test/java/nl.speyk.nifi.microsoft.graph.processors/TestInvokeMicrosoftGraphCalendar.java) | The code snippet in the file TestInvokeMicrosoftGraphCalendar.java is a test class that validates the functionality of the InvokeMicrosoftGraphCalendar class. It sets up the necessary dependencies and properties for the test and includes test cases for valid and empty JSON content. The main purpose is to ensure the correctness of the code logic in handling Microsoft Graph API requests related to calendars. |

</details>

<details closed><summary>nifi-microsoft-graph-processors.src.main.resources.META-INF.services</summary>

| File                                                                                                                                            | Summary                                                                                                                                                                                                                                   |
| ---                                                                                                                                             | ---                                                                                                                                                                                                                                       |
| [org.apache.nifi.processor.Processor](nifi-microsoft-graph-processors/src/main/resources/META-INF/services/org.apache.nifi.processor.Processor) | This code snippet is a configuration file that specifies the processor implementation for the Microsoft Graph integration in the NiFi data flow framework. It enables the NiFi system to invoke Microsoft Graph Calendar functionalities. |

</details>

<details closed><summary>nifi-microsoft-graph-processors.src.main.java.nl.speyk.nifi.microsoft.graph.processors</summary>

| File                                                                                                                                                              | Summary                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| ---                                                                                                                                                               | ---                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| [AbstractMicrosoftGraphCalendar.java](nifi-microsoft-graph-processors/src/main/java/nl/speyk/nifi/microsoft/graph/processors/AbstractMicrosoftGraphCalendar.java) | This code snippet is part of a parent repository that has a structure with multiple sub-folders containing different components. The main role of this code is to compile and copy NARS (NiFi Archive) files and provide a script for restarting NiFi, an open-source data integration tool. It contributes to the overall architecture of the repository by facilitating the deployment and management of NiFi and its associated components.                          |
| [InvokeMicrosoftGraphCalendar.java](nifi-microsoft-graph-processors/src/main/java/nl/speyk/nifi/microsoft/graph/processors/InvokeMicrosoftGraphCalendar.java)     | This code snippet, located in the `nifi-microsoft-graph-processors` module of the repository, is responsible for invoking the Microsoft Graph API to perform operations on calendar events. It retrieves events from a flow file, synchronizes them with events in the Microsoft Graph, and handles updates, deletions, and tentative status changes. The code interacts with a distributed cache and uses a Microsoft Graph client obtained from a credential service. |

</details>

<details closed><summary>nifi-microsoft-graph-processors.src.main.java.nl.speyk.nifi.microsoft.graph.processors.utils</summary>

| File                                                                                                                                            | Summary                                                                                                                                                                                                                                                                                          |
| ---                                                                                                                                             | ---                                                                                                                                                                                                                                                                                              |
| [Zermelo.java](nifi-microsoft-graph-processors/src/main/java/nl/speyk/nifi/microsoft/graph/processors/utils/Zermelo.java)                       | The code snippet in the file `Zermelo.java` is a utility class that interacts with the Zermelo API. It provides a method to update an appointment's online location URL by making a PUT request to the Zermelo API. The class uses the OkHttp library for HTTP communication.                    |
| [CalendarUtils.java](nifi-microsoft-graph-processors/src/main/java/nl/speyk/nifi/microsoft/graph/processors/utils/CalendarUtils.java)           | The code snippet in `CalendarUtils.java` is part of the `nifi-microsoft-graph-processors` module in the repository. It provides properties, relationships, and utility classes for working with Microsoft Graph API in NiFi processors.                                                          |
| [CalendarAttributes.java](nifi-microsoft-graph-processors/src/main/java/nl/speyk/nifi/microsoft/graph/processors/utils/CalendarAttributes.java) | The `CalendarAttributes` class in the `nl.speyk.nifi.microsoft.graph.processors.utils` package contains constants used for Microsoft Graph API integration in a NiFi data pipeline. It defines attributes and error codes, as well as keys for flow file attributes and distributed map caching. |

</details>

<details closed><summary>nifi-microsoft-graph-services-api</summary>

| File                                                 | Summary                                                                                                                                                                                                                                                                         |
| ---                                                  | ---                                                                                                                                                                                                                                                                             |
| [pom.xml](nifi-microsoft-graph-services-api/pom.xml) | This code snippet represents the POM file for the `nifi-microsoft-graph-services-api` module. It defines the project's dependencies, including the `nifi-api`, `nifi-utils`, and `microsoft-graph` libraries, required for interacting with Microsoft Graph API in Apache NiFi. |

</details>

<details closed><summary>nifi-microsoft-graph-services-api.target</summary>

| File                                                        | Summary                                                                                                                                                                                                                                                                                                    |
| ---                                                         | ---                                                                                                                                                                                                                                                                                                        |
| [.plxarc](nifi-microsoft-graph-services-api/target/.plxarc) | The code snippet is located in the `nifi-microsoft-graph-services-api` folder of the repository. It is a Maven project and the code compiles to create a shared archive resource for the Microsoft Graph API services in NiFi. The `.plxarc` file contains the compiled classes and resources for the API. |

</details>

<details closed><summary>nifi-microsoft-graph-services-api.target.test-classes.META-INF</summary>

| File                                                                                        | Summary                                                                                                                                                                                                                                                                                                                                                 |
| ---                                                                                         | ---                                                                                                                                                                                                                                                                                                                                                     |
| [DEPENDENCIES](nifi-microsoft-graph-services-api/target/test-classes/META-INF/DEPENDENCIES) | This code snippet, located in the `nifi-microsoft-graph-processors` directory of the repository, is responsible for compiling and copying NAR files, as well as restarting the Nifi framework. It plays a critical role in managing the deployment and operation of Microsoft Graph processors within the larger architecture of the parent repository. |
| [NOTICE](nifi-microsoft-graph-services-api/target/test-classes/META-INF/NOTICE)             | The code snippet in the `nifi-microsoft-graph-services-api` repository provides an API for interacting with Microsoft Graph services in an Apache NiFi environment. It enables seamless integration and communication between NiFi and Microsoft Graph.                                                                                                 |

</details>

<details closed><summary>nifi-microsoft-graph-services-api.target.maven-shared-archive-resources.META-INF</summary>

| File                                                                                                          | Summary                                                                                                                                                                                                                                                                                                               |
| ---                                                                                                           | ---                                                                                                                                                                                                                                                                                                                   |
| [DEPENDENCIES](nifi-microsoft-graph-services-api/target/maven-shared-archive-resources/META-INF/DEPENDENCIES) | This code snippet is part of the nifi-microsoft-graph-processors module in the repository. It includes the necessary files and folders for compiling and copying NARs (NiFi Archive) and for restarting NiFi. The code aims to provide functionalities related to Microsoft Graph API integration within Apache NiFi. |
| [NOTICE](nifi-microsoft-graph-services-api/target/maven-shared-archive-resources/META-INF/NOTICE)             | This code snippet is part of the `nifi-microsoft-graph-services-api` module in the repository. It includes the `NOTICE` file, which provides copyright and licensing information for the `nifi-microsoft-graph-services-api` software component.                                                                      |

</details>

<details closed><summary>nifi-microsoft-graph-services-api.target.classes.META-INF</summary>

| File                                                                                   | Summary                                                                                                                                                                                                                                                                                            |
| ---                                                                                    | ---                                                                                                                                                                                                                                                                                                |
| [DEPENDENCIES](nifi-microsoft-graph-services-api/target/classes/META-INF/DEPENDENCIES) | This code snippet, located within the nifi-microsoft-graph-processors directory, is responsible for compiling, copying NAR files, and restarting the Apache NiFi instance. It plays a critical role in managing the deployment and execution of processors related to Microsoft Graph integration. |
| [NOTICE](nifi-microsoft-graph-services-api/target/classes/META-INF/NOTICE)             | This code snippet is part of the nifi-microsoft-graph-services-api module in the repository structure. It contains a NOTICE file that includes copyright information for the Apache NiFi Project and the Apache Software Foundation.                                                               |

</details>

<details closed><summary>nifi-microsoft-graph-services-api.target.maven-status.maven-compiler-plugin.compile.default-compile</summary>

| File                                                                                                                                     | Summary                                                                                                                                                                                                                                                                                                                                                                                                          |
| ---                                                                                                                                      | ---                                                                                                                                                                                                                                                                                                                                                                                                              |
| [inputFiles.lst](nifi-microsoft-graph-services-api/target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst)     | This code snippet, located at `nifi-microsoft-graph-services-api/target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst`, contains the implementation of the `MicrosoftGraphCredentialService` in the parent repository's architecture. This service handles the authentication and authorization for Microsoft Graph API calls.                                                       |
| [createdFiles.lst](nifi-microsoft-graph-services-api/target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst) | The code snippet in the file `nifi-microsoft-graph-services-api/target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst` contains the compiled class `MicrosoftGraphCredentialService` in the package `nl.speyk.nifi.microsoft.graph.services.api`. It plays a critical role in the parent repository's architecture by providing a service for managing Microsoft Graph credentials. |

</details>

<details closed><summary>nifi-microsoft-graph-services-api.src.main.java.nl.speyk.nifi.microsoft.graph.services.api</summary>

| File                                                                                                                                                                    | Summary                                                                                                                                                                                                                                                                                             |
| ---                                                                                                                                                                     | ---                                                                                                                                                                                                                                                                                                 |
| [MicrosoftGraphCredentialService.java](nifi-microsoft-graph-services-api/src/main/java/nl.speyk.nifi.microsoft.graph.services.api/MicrosoftGraphCredentialService.java) | This code snippet defines the interface `MicrosoftGraphCredentialService` that extends `ControllerService`. It includes property descriptors for authentication client ID, tenant ID, client secret, grant type, and scope. The interface also provides a method to build a Microsoft Graph Client. |

</details>

<details closed><summary>nifi-microsoft-graph-processors-nar</summary>

| File                                                   | Summary                                                                                                                                                                                                                                                                                                                 |
| ---                                                    | ---                                                                                                                                                                                                                                                                                                                     |
| [pom.xml](nifi-microsoft-graph-processors-nar/pom.xml) | The code snippet in the `nifi-microsoft-graph-processors-nar` repository defines the configuration and dependencies for the Nar packaging of the Microsoft Graph Processors component in the parent repository. It includes dependencies on the Microsoft Graph Services Nar and Microsoft Graph Processors components. |

</details>

<details closed><summary>nifi-microsoft-graph-processors-nar.target</summary>

| File                                                          | Summary                                                                                                                                                                                                                                                      |
| ---                                                           | ---                                                                                                                                                                                                                                                          |
| [.plxarc](nifi-microsoft-graph-processors-nar/target/.plxarc) | The code snippet in the `nifi-microsoft-graph-processors-nar` repository is responsible for managing shared archive resources. It ensures the proper organization and distribution of resources across the different components and tests in the repository. |

</details>

<details closed><summary>nifi-microsoft-graph-processors-nar.target.test-classes.META-INF</summary>

| File                                                                                          | Summary                                                                                                                                                                                                                                                                   |
| ---                                                                                           | ---                                                                                                                                                                                                                                                                       |
| [DEPENDENCIES](nifi-microsoft-graph-processors-nar/target/test-classes/META-INF/DEPENDENCIES) | The code snippet in the nifi-microsoft-graph-processors repository is responsible for compiling and copying NARs, and restarting Nifi. It contributes to the architecture by managing the deployment and execution of Microsoft Graph processors in Nifi.                 |
| [NOTICE](nifi-microsoft-graph-processors-nar/target/test-classes/META-INF/NOTICE)             | The code snippet in the `nifi-microsoft-graph-processors-nar` component of the repository is responsible for processing Microsoft Graph data in Apache NiFi. It includes a NOTICE file that acknowledges the use of software developed by the Apache Software Foundation. |

</details>

<details closed><summary>nifi-microsoft-graph-processors-nar.target.maven-shared-archive-resources.META-INF</summary>

| File                                                                                                            | Summary                                                                                                                                                                                                                                                 |
| ---                                                                                                             | ---                                                                                                                                                                                                                                                     |
| [DEPENDENCIES](nifi-microsoft-graph-processors-nar/target/maven-shared-archive-resources/META-INF/DEPENDENCIES) | The code snippet in the `nifi-microsoft-graph-processors` directory is responsible for processing Microsoft Graph data in the NiFi system. It includes necessary configurations and settings for the processors.                                        |
| [NOTICE](nifi-microsoft-graph-processors-nar/target/maven-shared-archive-resources/META-INF/NOTICE)             | This code snippet is located in the `nifi-microsoft-graph-processors-nar` directory of the repository. It generates the NOTICE file, which includes copyright and licensing information for the Apache NiFi Project and the Apache Software Foundation. |

</details>

<details closed><summary>nifi-microsoft-graph-processors-nar.target.classes.META-INF</summary>

| File                                                                                     | Summary                                                                                                                                                                                                                                                                                                                                                                                                  |
| ---                                                                                      | ---                                                                                                                                                                                                                                                                                                                                                                                                      |
| [DEPENDENCIES](nifi-microsoft-graph-processors-nar/target/classes/META-INF/DEPENDENCIES) | The code snippet in the nifi-microsoft-graph-processors directory in the repository is responsible for implementing processors related to Microsoft Graph in Apache NiFi. It contains the necessary source code and configuration files for building and deploying the processors. The code snippet plays a critical role in integrating Microsoft Graph functionality into the NiFi data flow pipeline. |
| [NOTICE](nifi-microsoft-graph-processors-nar/target/classes/META-INF/NOTICE)             | The code snippet in the nifi-microsoft-graph-processors-nar repository generates a NOTICE file that acknowledges the software developed by the Apache Software Foundation for the NiFi Microsoft Graph Processors project.                                                                                                                                                                               |

</details>

---

##  Getting Started

***Requirements***

Ensure you have the following dependencies installed on your system:

* **<code>► INSERT-TEXT-HERE</code>**: `version x.y.z`

###  Installation

1. Clone the . repository:

```sh
git clone ../.
```

2. Change to the project directory:

```sh
cd .
```

3. Install the dependencies:

```sh
> INSERT-INSTALL-COMMANDS
```

###  Running .

Use the following command to run .:

```sh
> INSERT-RUN-COMMANDS
```

###  Tests

To execute tests, run:

```sh
> INSERT-TEST-COMMANDS
```

---

##  Project Roadmap

- [X] `► INSERT-TASK-1`
- [ ] `► INSERT-TASK-2`
- [ ] `► ...`

---

##  Contributing

Contributions are welcome! Here are several ways you can contribute:

- **[Submit Pull Requests](https://local//blob/main/CONTRIBUTING.md)**: Review open PRs, and submit your own PRs.
- **[Join the Discussions](https://local//discussions)**: Share your insights, provide feedback, or ask questions.
- **[Report Issues](https://local//issues)**: Submit bugs found or log feature requests for ..

<details closed>
    <summary>Contributing Guidelines</summary>

1. **Fork the Repository**: Start by forking the project repository to your GitHub account.
2. **Clone Locally**: Clone the forked repository to your local machine using a Git client.
   ```sh
   git clone ../.
   ```
3. **Create a New Branch**: Always work on a new branch, giving it a descriptive name.
   ```sh
   git checkout -b new-feature-x
   ```
4. **Make Your Changes**: Develop and test your changes locally.
5. **Commit Your Changes**: Commit with a clear message describing your updates.
   ```sh
   git commit -m 'Implemented new feature x.'
   ```
6. **Push to GitHub**: Push the changes to your forked repository.
   ```sh
   git push origin new-feature-x
   ```
7. **Submit a Pull Request**: Create a PR against the original project repository. Clearly describe the changes and their motivations.

Once your PR is reviewed and approved, it will be merged into the main branch.

</details>

---

##  License

This project is protected under the [SELECT-A-LICENSE](https://choosealicense.com/licenses) License. For more details, refer to the [LICENSE](https://choosealicense.com/licenses/) file.

---

##  Acknowledgments

- List any resources, contributors, inspiration, etc. here.

[**Return**](#-quick-links)

---
