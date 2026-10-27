# Model pracy zespołu: BusRadar Gdańsk

## 1. Zasada ogólna

Zespół pracuje w modelu właścicieli obszarów. Każdy obszar systemu ma jednego właściciela, który odpowiada za jego projekt, spójność i jakość, oraz zastępcę, który go wspiera.

Zadania nie są przypisywane z góry. Każdy członek zespołu bierze z tablicy dowolne zadanie, również z obszaru innej osoby. Właściciel obszaru nie musi realizować wszystkich jego zadań sam; odpowiada za to, żeby obszar był zaprojektowany, rozpisany na zadania i dobrze wykonany.

Model ma trzy cele: każdy uczestnik ma własny, widoczny wkład w projekt; nikt nie czeka bezczynnie na innych; każdy poznaje też części systemu spoza swojego obszaru.

## 2. Obszary, właściciele i zastępcy

System dzielimy na sześć obszarów. Każdy uczestnik jest właścicielem jednego lub dwóch obszarów i zastępcą w jednym lub dwóch innych.

| Obszar                              | Właściciel         | Zastępca           | Zakres                                                                                                                                                        |
|-------------------------------------|--------------------|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Strumień zdarzeń i czas rzeczywisty | Jakub Jurkian      | Kacper Korzekwa    | Apache Kafka (topiki, format zdarzeń, obsługa błędów, dead-letter topic), wykrywanie przejazdów przez przystanki, bieżący stan pojazdów w Redis, strumień SSE |
| Frontend                            | Jakub Jurkian      | Tomasz Dylik       | struktura aplikacji Angular, konwencje i komponenty wspólne, mapa na żywo, spójność wszystkich widoków                                                        |
| Dane i analityka                    | Kacper Korzekwa    | Marcel Jaroszewicz | schemat PostgreSQL (partycje, retencja, migracje Flyway), historia przejazdów, statystyki punktualności, rankingi, typowe opóźnienia                          |
| Integracja z ZTM                    | Marcel Jaroszewicz | Kacper Korzekwa    | pobieranie danych z API ZTM, deduplikacja, odporność na awarie API, import listy przystanków z GTFS, nagrywanie danych i tryb odtwarzania                     |
| Infrastruktura i jakość             | Marcel Jaroszewicz | Tomasz Dylik       | Docker, Docker Compose, CI/CD, serwer 24/7, monitoring, strategia testów, testy E2E                                                                           |
| Użytkownicy i bezpieczeństwo        | Tomasz Dylik       | Jakub Jurkian      | Keycloak, logowanie, autoryzacja w serwisach, „moje przejazdy”                                                                                                |

Podział uwzględnia zainteresowania zespołu: Jakub rozwija się w kierunku przetwarzania strumieni i fullstacku, Kacper i Marcel w kierunku backendu i DevOps, Tomasz łączy rolę team leada z backendem i frontendem. Obszary można zmienić decyzją zespołu na retrospektywie.

## 3. Role

### Właściciel obszaru

- Projektuje rozwiązania w swoim obszarze i dokumentuje kluczowe decyzje w ADR.

- Rozpisuje prace ze swojego obszaru na zadania i dba, żeby na tablicy były zadania gotowe do wzięcia.

- Jest obowiązkowym recenzentem pull requestów zmieniających jego obszar.

- Odpowiada na pytania i pomaga osobom, które biorą zadania z jego obszaru.

- Na środowym spotkaniu przedstawia stan swojego obszaru.

### Zastępca

- Zna obszar na tyle, żeby samodzielnie brać z niego zadania.

- Może zatwierdzać pull requesty w obszarze, gdy właściciel nie odpowie w ciągu 48 godzin albo do środy 14:00.

- Przejmuje decyzje w obszarze podczas nieobecności właściciela.

### Team lead (Tomasz Dylik)

- Prowadzi tablicę zadań i spotkania zespołu.

- Pilnuje terminów kamieni milowych i kontaktuje się z prowadzącym.

- Rozstrzyga sprawy między obszarami, gdy zespół nie osiągnie zgody.

## 4. Praca z zadaniami

Zadania prowadzimy na tablicy w GitHub Projects. Każde zadanie ma etykietę obszaru i trwa od jednego do trzech wieczorów pracy; większe dzielimy.

### Kolumny tablicy

1.  **Backlog**: pomysły i ogólne punkty, jeszcze nierozpisane.

2.  **Gotowe do wzięcia**: zadania spełniające Definition of Ready.

3.  **W toku**: zadanie ma przypisaną osobę.

4.  **Review**: otwarty pull request czeka na przegląd.

5.  **Zrobione**: zadanie spełnia Definition of Done.

### Zasady

- Zadanie bierzesz, przypisując się do niego na tablicy, zanim zaczniesz pracę.

- Jedna osoba ma naraz najwyżej dwa zadania w kolumnie „W toku”.

- Każdy uczestnik pracuje głównie w swoich obszarach, ale co dwa tygodnie bierze co najmniej jedno zadanie z obszaru innej osoby.

- Jeśli zadanie stoi w miejscu dłużej niż tydzień, osoba zgłasza to na Discordzie, żeby zespół mógł pomóc albo przejąć zadanie.

### Definition of Ready

Zadanie trafia do kolumny „Gotowe do wzięcia”, gdy:

- ma jasny opis i warunek „gotowe, gdy…”,

- nie zależy od niezrobionej pracy albo zależność zastępuje atrapa (mock, nagrany plik danych, przykładowa odpowiedź z OpenAPI),

- właściciel obszaru je zaakceptował.

### Definition of Done

Zadanie jest zrobione, gdy:

- pull request przeszedł review właściciela obszaru (lub zastępcy),

- testy przechodzą w CI, a nowa logika ma testy,

- zmiana jest scalona do `main` i wdrożona na serwer,

- dokumentacja (README, OpenAPI, ADR) jest aktualna, jeśli zmiana jej dotyczy.

## 5. Decyzje, ADR i przegląd kodu

### Decyzje

- Decyzje w obrębie jednego obszaru podejmuje jego właściciel.

- Decyzje dotyczące kilku obszarów (format zdarzeń w Kafce, kontrakty REST API, schemat bazy używany przez kilka serwisów) zespół podejmuje wspólnie. Gdy nie ma zgody, rozstrzyga team lead.

- Kontrakt między obszarami ustalamy, zanim powstanie kod, który z niego korzysta. Dzięki temu obie strony pracują równolegle.

### ADR

Kluczowe decyzje zapisujemy jako krótkie dokumenty ADR (Architecture Decision Record) w katalogu `docs/adr/` w repozytorium. Każdy ADR ma numer, tytuł, kontekst, decyzję, rozważane alternatywy i konsekwencje. Pierwszy ADR (ADR-001) opisuje format zdarzeń i topiki Kafki.

### Przegląd kodu

- Każda zmiana trafia do `main` przez pull request; bezpośredni push do `main` jest zablokowany.

- Pull request wymaga akceptacji właściciela obszaru. Wymusza to plik `CODEOWNERS`, który przypisuje katalogi repozytorium do właścicieli.

- Gdy właściciel nie odpowie w ciągu 48 godzin albo do środy 14:00, pull request może zatwierdzić zastępca.

- Pull request ma krótki opis: co zmienia, jak to sprawdzić, z którym zadaniem jest powiązany.

## 6. Rytm pracy

Pracujemy w cyklu tygodniowym od środy do środy. Na żywo rozmawiamy tylko w środę i ewentualnie w sobotę; cała reszta dzieje się na tablicy zadań i na Discordzie. Zadania każdy realizuje w dowolnym czasie w tygodniu, ale na środowe spotkanie przychodzi z wykonaną pracą.

### Tydzień pracy

| Kiedy                    | Co                                                                                     | Udział                       |
|--------------------------|----------------------------------------------------------------------------------------|------------------------------|
| środa, 16:00 (max 1,5 h) | spotkanie tygodniowe: stan projektu, decyzje, plan na tydzień                          | obowiązkowy                  |
| czwartek–wtorek          | praca własna nad zadaniami z tablicy, w dowolnym czasie                                | —                            |
| piątek, do 20:00         | zgłoszenie tematów na sobotnią rozmowę na Discordzie                                   | —                            |
| sobota, 10:00 (max 1 h)  | rozmowa na Discordzie o sprawach technicznych; odbywa się tylko, gdy ktoś zgłosi temat | osoby, których temat dotyczy |
| niedziela, wieczór       | ostatni moment na zgłoszenie, że zadanie tygodnia jest zagrożone                       | —                            |
| wtorek, do 22:00         | pull requesty z zadaniami tygodnia są otwarte                                          | obowiązkowy                  |
| środa, do 14:00          | recenzenci przeglądają otwarte pull requesty                                           | właściciele i zastępcy       |

### Spotkanie środowe

Spotkanie prowadzi team lead i trzyma się limitu 1,5 godziny.

1.  **Stan prac (ok. 30 min):** każdy pokazuje, co zrobił od poprzedniej środy, najlepiej na działającym systemie lub w pull requestcie.

2.  **Przegląd tablicy (ok. 15 min):** pull requesty czekające na review, blokady, zadania stojące dłużej niż tydzień.

3.  **Decyzje (ok. 20 min):** sprawy między obszarami, kontrakty, ADR do akceptacji.

4.  **Plan na tydzień (ok. 20 min):** każdy bierze zadania z kolumny „Gotowe do wzięcia” i mówi, co dowiezie na kolejną środę.

5.  **Retrospektywa (co drugie spotkanie, ok. 10 min):** co działa, co poprawiamy.

Po spotkaniu team lead wrzuca na Discorda krótką notatkę: podjęte decyzje i zadania wzięte na tydzień.

### Zasada: nie przychodzimy z pustymi rękami

- Na każdą środę każdy ma co najmniej jedno zadanie w kolumnie „Review” lub „Zrobione”.

- Zadania na tydzień dobierasz do czasu, który realnie masz. Lepiej jedno małe zadanie dowiezione niż trzy rozgrzebane. Tydzień z mniejszą dostępnością (kolokwia, praca) zgłaszasz już na środowym spotkaniu.

- Jeśli w trakcie tygodnia widzisz, że nie zdążysz, piszesz o tym na Discordzie najpóźniej w niedzielę wieczorem. Wtedy można zmniejszyć zadanie, poprosić o pomoc albo przekazać je komuś innemu.

- Blokadę (czekasz na kogoś, coś nie działa) zgłaszasz od razu na kanale #pomoc, nie dopiero w środę.

### Discord

- **#ogólne:** bieżąca komunikacja zespołu.

- **#pomoc:** blokady i pytania do właścicieli obszarów.

- **#spotkania:** tematy na sobotę i notatki ze spotkań.

- **#github:** automatyczne powiadomienia o pull requestach i zadaniach.

Discord służy do rozmowy, nie do dokumentacji. Ustalenia, które dotyczą innych osób, zapisujemy w opisie zadania albo w ADR.

### Etapy

Tygodnie łączymy w etapy zgodne z kamieniami milowymi z karty projektu.

| Etap                      | Spotkania                       | Cel                                                                                 |
|---------------------------|---------------------------------|-------------------------------------------------------------------------------------|
| 0. Przygotowanie         | sob. 24.10 (Discord), śr. 28.10 | karta projektu (do 26.10), środowisko, ADR-001, backlog                             |
| 1. Walking skeleton      | śr. 04.11, śr. 11.11 (święto)   | najprostszy przepływ danych działający na serwerze, do 17.11                        |
| 2. Zbieranie danych      | śr. 18.11, śr. 25.11            | pełne pobieranie i zapis danych, mapa na żywo, logowanie; zbieranie danych od 23.11 |
| 3. Funkcjonalność główna | śr. 02.12, śr. 09.12            | statystyki, rankingi, „moje przejazdy”, tryb odtwarzania, do 14.12                  |
| 4. Testy i poprawki      | śr. 16.12, śr. 06.01 (święto)   | testy integracyjne i E2E, poprawki, funkcje opcjonalne                              |
| 5. Finalizacja           | śr. 13.01, śr. 20.01            | dokumentacja (do 18.01), stabilizacja, prezentacja końcowa ok. 25.01                |

W środy 23.12 i 30.12 nie ma spotkań. O spotkaniach w święta, 11.11 (Święto Niepodległości) i 06.01 (Trzech Króli), zespół decyduje tydzień wcześniej: online w tym samym terminie albo w najbliższą sobotę (14.11, 09.01).

## 7. Start projektu

Pierwsze dwa etapy nie dostarczają funkcji, tylko fundament: wspólne ustalenia, środowisko i najcieńszy działający przekrój przez cały system.

### Spotkania startowe

Kartę trzeba oddać do 26.10, czyli przed pierwszą środą, dlatego start dzielimy na dwa spotkania. W sobotę 24.10 o 10:00 na Discordzie (1 h) zespół akceptuje kartę projektu i ten dokument. W środę 28.10 o 16:00 (1,5 h) omawia punkty poniżej.

1.  Potwierdzenie celu projektu i zakresu MVP z karty projektu.

2.  Mapa historyjek użytkownika: wypisujemy, co użytkownik robi w aplikacji (np. „widzę autobusy na mapie”, „sprawdzam typowe opóźnienie mojej linii”), i układamy to w kolejności ważności.

3.  Akceptacja tego dokumentu: obszary, właściciele, zasady pracy.

4.  Wstępne kontrakty między obszarami: jakie zdarzenia płyną przez Kafkę i jakie endpointy udostępnia każdy serwis.

### Zadania etapu 0

Zadania przygotowują właściciele obszarów, ale może je wziąć każdy.

- [ ] Repozytorium: ochrona gałęzi `main`, `CODEOWNERS`, szablon pull requesta (Infrastruktura i jakość)

- [ ] Docker Compose z Kafką, PostgreSQL, Redis i Keycloakiem (Infrastruktura i jakość)

- [ ] CI w GitHub Actions: budowanie i testy dla każdego pull requesta (Infrastruktura i jakość)

- [ ] Szkielety serwisów Spring Boot i wspólny moduł `events` (Strumień zdarzeń)

- [ ] ADR-001: format zdarzeń i topiki Kafki (Strumień zdarzeń)

- [ ] Szkielet aplikacji Angular z mapą Leaflet (Frontend)

- [ ] Nagranie próbki danych z API ZTM do pliku, sprawdzenie licencji danych (Integracja z ZTM)

- [ ] Wstępny schemat bazy i pierwsza migracja Flyway (Dane i analityka)

- [ ] Realm i klient aplikacji w Keycloaku (Użytkownicy i bezpieczeństwo)

- [ ] Tablica w GitHub Projects z backlogiem na etap 1 (team lead)

### Walking skeleton (etap 1)

Celem etapu 1 jest najprostsza wersja całego przepływu danych, wdrożona na serwer: ingest-service pobiera pozycje z API ZTM i publikuje je do Kafki, tracking-service zapisuje ostatnią pozycję w Redis i wysyła ją przez SSE, a aplikacja Angular pokazuje pojazdy na mapie. Równolegle stats-service zapisuje odczyty do PostgreSQL.

Każdy obszar dokłada do szkieletu swój minimalny kawałek, bez obsługi błędów i bez dopracowania. Gdy szkielet działa od końca do końca, właściciele obszarów rozpisują kolejne zadania, a każdy może pracować równolegle na sprawdzonej architekturze.

Backlog rozpisujemy stopniowo: szczegółowo tylko na najbliższe dwa tygodnie, a dalsze prace jako ogólne punkty, które właściciel obszaru dzieli na zadania przed środowym spotkaniem.
