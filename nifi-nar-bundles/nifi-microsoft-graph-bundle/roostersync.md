De roostersynchronisatie ondersteunt verschillende scenario's, afhankelijk van de synchronisatiefrequentie:

### Volledige synchronisatie (elke nacht):
Deze synchronisatie omvat alle afspraken voor de komende 3/6 weken die als geldig zijn gemarkeerd.

1. **Nieuwe afspraak in Zermelo, maar niet in de agenda:**
   - Actie: Afspraak wordt aangemaakt in de agenda.

2. **Afspraak niet geldig (valid=false) in Zermelo:**
   - Actie: Afspraak wordt op 'tentative' gezet of verwijderd op basis van de geconfigureerde parameterwaarde.

3. **Afspraak bestaat in zowel Zermelo als de agenda:**
   - Actie: Geen toevoeging of verwijdering.

4. **Afspraak bestaat zowel in Zermelo als de agenda, maar inhoud verschilt:**
   - Actie: Agenda-afspraak wordt aangepast aan Zermelo. Geannuleerde afspraken worden grijs gearceerd in de agenda met de inhoud "Les vervalt." wanneer geopend. In de beschrijving worden de karakters [!] toegevoegd. Een bestaande teams-link in de inhoud van de afsrpaak blijft ongewijzigd.

### Snelle synchronisatie (elke 5 minuten):
Deze synchronisatie omvat alle afspraken van de afgelopen 15 minuten.

1. **Nieuwe afspraak in Zermelo, maar niet in de agenda:**
   - Actie: Afspraak wordt aangemaakt in de agenda.

2. **Afspraak bestaat zowel in Zermelo als de agenda:**
   - Actie: Geen toevoeging of verwijdering.

3. **Afspraak bestaat zowel in Zermelo als de agenda, maar inhoud verschilt:**
   - Actie: Agenda-afspraak wordt aangepast aan Zermelo. Geannuleerde afspraken worden grijs gearceerd in de agenda met de inhoud "Les vervalt." wanneer geopend. In de beschrijving worden de karakters [!] toegevoegd. Een bestaande teams-link in de inoud van de afspraak blijft ongewijzigd.


### Speciaal scenario:
- **Opslag van digitale vingerafdrukken:**
   - Actie: Vingerafdrukken van alle afspraken worden opgeslagen in een gedistribueerde mapcache.

- **Periodieke verificatie van vingerafdrukken (elke 5 minuten):**
   - Actie: Vingerafdrukken van agenda-afspraken worden vergeleken met die van Zermelo alleen in het geval dat er nieuwe of gewijzigde afsrpaken zijn. Bij een verschil wordt de agenda-afspraak aangepast aan de afspraak in Zermelo. De inhoud van de afspraak blijft ongewijzigd.

### Opmerking:
Andere scenario's worden op dit moment niet ondersteund.
