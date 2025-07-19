# CRUD Generator Plugin

Il **CRUD Generator Plugin** è un plugin Maven che consente di generare automaticamente le operazioni CRUD (Create, Read, Update, Delete) per i modelli Java definiti nel progetto. Utilizza **MapStruct** per la mappatura tra entità e DTO (Data Transfer Objects), rendendo lo sviluppo più rapido e meno soggetto a errori.

## Come funziona

Il plugin genera automaticamente i seguenti file a partire da una classe del modello specificata:
- **DTOs** (Data Transfer Objects).
- **Mapper** per la conversione tra entità e DTO.
- **Repository** (interfaccia per l'accesso al database).
- **Servizi** che contengono la logica di business.
- **Controller** per gestire endpoint REST.

Ogni componente può essere incluso o escluso a seconda delle esigenze, grazie a una serie di parametri configurabili.

## Uso del plugin

### Comando generico

Per eseguire il plugin è sufficiente utilizzare il comando Maven:
```shell
mvn it.fabiodezuani:mvn-crud-generator:1.0:generate-crud -DmodelClass=path_al_modello -DrootPackage=path_al_package_root [altri_parametri_opzionali]
```


### Esempi pratici

Di seguito alcuni esempi di utilizzo del comando Maven:

1. Generazione completa delle CRUD per la classe `model.Auto`:
   ```shell
   mvn it.fabiodezuani:mvn-crud-generator:1.0:generate-crud -DmodelClass=model.Auto -DrootPackage=it.fabiodezuani.demomvnplugingenerator
   ```

2. Generazione parziale escludendo alcuni componenti (DTO, repository, service e controller):
   ```shell
   mvn it.fabiodezuani:mvn-crud-generator:1.0:generate-crud -DmodelClass=model.Auto -DrootPackage=it.fabiodezuani.demomvnplugingenerator -DskipDto=true -DskipRepository=true -DskipService=true -DskipController=true -DskipMapper=true
   ```

Con questi comandi si può configurare completamente la generazione in base alle esigenze del progetto.

## Parametri configurabili

I parametri del plugin possono essere personalizzati attraverso le opzioni seguenti:

### Parametri principali

- **`-DmodelClass`** (Obbligatorio): Il percorso completo della classe del modello per la quale si vogliono generare le CRUD.  
  *Esempio*: `-DmodelClass=model.Auto`

- **`-DrootPackage`** (Obbligatorio): Il pacchetto root del progetto. Questo verrà utilizzato per generare i pacchetti destinazione per i file generati.  
  *Esempio*: `-DrootPackage=it.fabiodezuani.demomvnplugingenerator`

### Parametri opzionali

- **`-DskipDto`**: Esclude la generazione dei DTO.  
  *Valore predefinito*: `false` (generazione abilitata).  
  *Esempio*: `-DskipDto=true`

- **`-DskipRepository`**: Esclude la generazione del repository.  
  *Valore predefinito*: `false`.  
  *Esempio*: `-DskipRepository=true`

- **`-DskipService`**: Esclude la generazione del service.  
  *Valore predefinito*: `false`.  
  *Esempio*: `-DskipService=true`

- **`-DskipController`**: Esclude la generazione del controller REST.  
  *Valore predefinito*: `false`.  
  *Esempio*: `-DskipController=true`

- **`-DskipMapper`**: Esclude la generazione del mapper (MapStruct).  
  *Valore predefinito*: `false`.  
  *Esempio*: `-DskipMapper=true`

- **`-Dmapper`**: Specifica il tipo di mapper da utilizzare.  
  *Valori possibili*: `MAPSTRUCT` (predefinito).

- **`-DoutputDir`**: Directory di output per i file generati.  
  *Valore predefinito*: `${project.basedir}/src/main/java`.

- **`-DoverrideFileCreation`**: Sovrascrive i file esistenti durante la generazione.  
  *Valore predefinito*: `false`.  
  *Esempio*: `-DoverrideFileCreation=true`

## Dettagli tecnici

Il plugin utilizza **JavaPoet** per generare il codice e analizza la classe modello tramite **Reflection** per estrarre le informazioni necessarie, come:
- Nome della classe.
- Pacchetto di appartenenza.
- Campi della classe, incluso il supporto per tipi complessi come collezioni o campi annidati.

I file generati vengono salvati nella directory indicata da `-DoutputDir`.

## Messaggi informativi

Durante l'esecuzione, il plugin fornisce diversi messaggi di log che mostrano lo stato della generazione, come:
- Conferma del caricamento della classe del modello.
- Informazioni sui file generati (DTOs, repository, mapper, ecc.).
- Eventuali errori in caso di problemi (es. classe non trovata).

## Conclusioni

Il **CRUD Generator Plugin** offre un modo rapido e automatizzato per creare l'intera struttura di un'applicazione basata su CRUD. È possibile configurarlo facilmente tramite i parametri forniti, garantendo flessibilità e controllo sull'output generato.
