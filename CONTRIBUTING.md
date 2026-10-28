# Workflow pracy: BusRadar Gdańsk

Opis jak tworzymy, bierzemy, realizujemy i przeglądamy zadania.

Uzupełnia dokumenty zespołu w `docs/` (model pracy zespołu, przebieg systemu, konwencje repozytorium).

---

## 1. Język

Cały kod, komentarze w kodzie, nazwy gałęzi, commity, zgłoszenia (Issues) i Pull Requesty piszemy **po angielsku**. Dotyczy to też dokumentacji technicznej w repozytorium: README, ADR, opisów API.

Chcemy pracować tak, jak zespół w anglojęzycznym środowisku, i trzymać się standardów branżowych. Po polsku mogą zostać tylko wewnętrzne dokumenty organizacyjne zespołu, takie jak ten.

---

## 2. Przebieg zadania

```
Backlog ──▶ To Do ──▶ In Progress ──▶ In Review ──▶ Done
(pomysł)   (gotowe    (przypisane,    (PR otwarty,   (scalone,
           do wzięcia) gałąź otwarta)  czeka na       wdrożone)
                                       review)
```

|Kolumna|Znaczenie|Kto przesuwa|
|-|-|-|
|**Backlog**|Pomysły, przyszłe zadania i funkcje opcjonalne, jeszcze nie zaplanowane.|każdy|
|**To Do**|Zadania na bieżący etap, które spełniają *Definition of Ready* i można je wziąć.|właściciel obszaru|
|**In Progress**|Ktoś aktywnie pracuje nad zadaniem.|osoba przypisana|
|**In Review**|Kod jest gotowy, Pull Request czeka na review.|osoba przypisana|
|**Done**|PR jest zatwierdzony i scalony do `main`; zadanie spełnia *Definition of Done*.|automatycznie (merge PR zamyka issue)|

### Krok po kroku

1. **Załóż issue** z jednego z szablonów (feature, bug, task). Dodaj dokładnie jedną etykietę `area:` i jedną `type:`. Nowe issue trafia do **Backlog**.
2. **Doprecyzowanie.** Właściciel obszaru sprawdza issue według *Definition of Ready* i przesuwa je do **To Do**.
3. **Weź zadanie.** Przypisz się, przesuń kartę do **In Progress** i utwórz gałąź:
`git switch -c feature/12-kafka-topics`
Naraz możesz mieć najwyżej **2 zadania w toku**.
4. **Pracuj małymi commitami** w formacie Conventional Commits (sekcja 6). Wypychaj zmiany wcześnie. Jeśli chcesz opinii przed skończeniem, otwórz PR jako *draft*.
5. **Otwórz Pull Request** z szablonu, **gdy tylko zadanie jest gotowe**, z `Closes #12` w opisie, i przesuń kartę do **In Review**. Nie czekaj z tym do końca tygodnia. Ostateczny termin to **wtorek 22:00**, żeby zadanie zostało przejrzane przed środowym spotkaniem.
6. **Review.** PR przegląda właściciel obszaru albo zastępca (obaj są wpisani w `CODEOWNERS`, wystarczy akceptacja jednego), **w miarę możliwości w ciągu 24 godzin**, najpóźniej w ciągu 48 godzin. PR-y otwarte do wtorku 22:00 muszą być przejrzane **do środy 14:00**. Gdy autor jest właścicielem obszaru, review robi zastępca. Zadania `docs` i `chore` może zatwierdzić dowolna osoba z zespołu.
7. **Merge.** Po akceptacji i zielonym CI autor scala PR opcją **Squash and merge** i usuwa gałąź. Issue zamyka się automatycznie, a karta przechodzi do **Done**.

### Gdy coś idzie nie tak

* **Blokada** (czekasz na kogoś albo coś nie działa): dodaj etykietę `blocked` i napisz na `#pomoc` na Discordzie **od razu**, nie dopiero w środę.
* **Nie zdążysz w tym tygodniu:** napisz o tym na Discordzie **najpóźniej w niedzielę wieczorem**, żeby zadanie można było podzielić, przekazać albo komuś pomóc.
* **Potrzebna decyzja zespołu:** dodaj etykietę `needs-decision`; temat trafi na środowe spotkanie.

---

## 3. Definition of Ready

Issue może trafić do **To Do**, gdy:

* [ ] opis mówi, co trzeba zrobić i dlaczego,
* [ ] ma jasne kryteria akceptacji („gotowe, gdy…”),
* [ ] ma jedną etykietę `area:` i jedną `type:`,
* [ ] nie zależy od niezrobionej pracy albo zależność zastępuje mock, nagrane dane lub uzgodniony kontrakt API,
* [ ] mieści się w ok. 1–3 wieczorach pracy; większe zadania dzielimy.

## 4. Definition of Done

Zadanie jest zrobione, gdy:

* [ ] kryteria akceptacji są spełnione,
* [ ] PR zatwierdził właściciel obszaru albo zastępca (przy `docs` i `chore`: dowolna osoba z zespołu),
* [ ] CI jest zielone, a nowa logika ma testy,
* [ ] zmiana jest scalona do `main` i wdrożona,
* [ ] dokumentacja (README, OpenAPI, ADR) jest zaktualizowana, jeśli zmiana jej dotyczy.

---

## 5. Nazwy gałęzi

Format: **`<typ>/<nr-issue>-<krótki-opis>`**, małe litery, słowa oddzielone myślnikami, opis po angielsku.

|Typ|Kiedy|Przykład|
|-|-|-|
|`feature/`|nowa funkcja|`feature/12-kafka-topics`|
|`fix/`|poprawka błędu|`fix/34-duplicate-positions`|
|`chore/`|konfiguracja, zależności, CI|`chore/5-ci-pipeline`|
|`docs/`|dokumentacja, ADR|`docs/8-adr-001-events`|
|`refactor/`|zmiana bez nowego zachowania|`refactor/41-stats-queries`|
|`test/`|same testy|`test/27-ingest-wiremock`|

Gałęzie są krótkie: żyją od wzięcia zadania do merge'a. Po merge'u je usuwamy.

---

## 6. Commity

Stosujemy [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary in imperative mood>
```

* **type:** `feat`, `fix`, `chore`, `docs`, `refactor`, `test`
* **scope:** część systemu: `ingest`, `tracking`, `stats`, `trips`, `events`, `frontend`, `infra`
* **opis:** po angielsku, w trybie rozkazującym, małymi literami, bez kropki na końcu, maks. ok. 72 znaki

Przykłady:

```
feat(tracking): publish stop passages to stop-passages topic
fix(ingest): skip positions with duplicate timestamp
chore(infra): add healthcheck for kafka in docker compose
docs(adr): add ADR-001 kafka topics and event format
test(stats): cover punctuality query with testcontainers
```

Ponieważ scalamy przez squash, **tytuł PR-a** staje się commitem na `main`, więc musi mieć ten sam format.



---

## 7. Etykiety

Każde issue ma **jedną etykietę obszaru** i **jedną etykietę typu**. Status śledzimy kolumnami tablicy, nie etykietami.

|Etykieta obszaru|Obszar|Właściciel|
|-|-|-|
|`area: streaming`|Strumień zdarzeń i czas rzeczywisty|Jakub Jurkian|
|`area: frontend`|Frontend|Jakub Jurkian|
|`area: data`|Dane i analityka|Kacper Korzekwa|
|`area: ztm`|Integracja z ZTM|Marcel Jaroszewicz|
|`area: infra`|Infrastruktura i jakość|Marcel Jaroszewicz|
|`area: auth`|Użytkownicy i bezpieczeństwo|Tomasz Dylik|

|Etykieta typu|Kiedy|
|-|-|
|`type: feature`|nowa funkcja|
|`type: bug`|coś nie działa|
|`type: chore`|konfiguracja, zależności, CI|
|`type: docs`|dokumentacja, ADR|
|`type: spike`|zadanie badawcze; wynikiem jest notatka albo ADR, a nie kod|

Dodatkowe etykiety: `blocked`, `needs-decision`.

Etykiety zakładamy ręcznie w ustawieniach repozytorium (Issues → Labels).

---

## 8. Szablony issue

Treść szablonów jest po angielsku, bo tak piszemy issue.

### `.github/ISSUE_TEMPLATE/feature.md`

```markdown
---
name: Feature
about: Propose a new feature or user-facing change
title: 'feat(<scope>): '
labels: 'type: feature'
assignees: ''
---

## Description
<!-- What and why. Prefer a user story:
"As a passenger, I want to see buses on a live map so that I know when to leave home." -->

## Acceptance Criteria
<!-- The task is done when... Each item must be verifiable. -->
- [ ] 
- [ ] 

## Technical Notes
<!-- Hints, constraints, links to ADRs or API contracts.
E.g. "Use Leaflet", "Consumer group: tracking", "See ADR-001". -->

## Dependencies
<!-- Blocked by #..., or the mock / recorded data to use instead. Write "None" if independent. -->

## Size
<!-- S (≤ 1 evening) / M (2–3 evenings). Anything larger must be split. -->
```

### `.github/ISSUE_TEMPLATE/bug.md`

```markdown
---
name: Bug
about: Report something that does not work as expected
title: 'fix(<scope>): '
labels: 'type: bug'
assignees: ''
---

## Description
<!-- A clear and concise description of the bug. -->

## Steps to Reproduce
1. 
2. 
3. 

## Expected Behavior
<!-- What should happen? -->

## Actual Behavior
<!-- What happens instead? Include logs, stack traces or screenshots. -->

## Environment
<!-- local (docker compose) / server; branch or commit; browser if frontend. -->
```

### `.github/ISSUE_TEMPLATE/task.md`

Na pracę, która nie jest ani funkcją, ani błędem: konfigurację, dokumentację, zadania badawcze.

```markdown
---
name: Task
about: Chore, documentation or research (spike)
title: ''
labels: ''
assignees: ''
---

## Goal
<!-- What needs to be done and why. -->

## Done When
- [ ] 
- [ ] 

## Notes
<!-- Links, context. For a spike: the question to answer and where the result goes (note, ADR). -->
```

### `.github/ISSUE_TEMPLATE/config.yml`

Wyłącza puste issue, więc każde zaczyna się od szablonu.

```yaml
blank_issues_enabled: false
```

---

## 9. Szablon Pull Requesta

### `.github/PULL_REQUEST_TEMPLATE.md`

```markdown
## Context
<!-- Why is this change needed? -->

Closes #

## What Changed
<!-- The technical changes and any design decisions made in this PR. -->
- 
- 

## How to Test
<!-- Steps for the reviewer to verify the change locally. -->
1. 

## Screenshots
<!-- Frontend changes only. Delete this section otherwise. -->

## Checklist
- [ ] PR title follows Conventional Commits (it becomes the commit on `main`)
- [ ] Tests added or updated, and passing locally
- [ ] Application builds and starts with `docker compose up`
- [ ] Documentation / ADR updated, or not needed
- [ ] No secrets, tokens or `.env` values committed

## AI Usage
<!-- "None", or what AI was used for, e.g. "Claude generated the Kafka consumer config; reviewed and adjusted manually." -->

## Notes for the Reviewer
<!-- Areas to focus on, open questions, technical debt left on purpose. -->
```

---

## 10. Code review

**Jako autor:**

* Rób małe PR-y, najlepiej poniżej ok. 400 zmienionych linii. Duże PR-y dostają gorsze review.
* Przejrzyj własny diff, zanim poprosisz o review.
* Odpowiedz na każdy komentarz: popraw albo wyjaśnij, dlaczego nie.

**Jako recenzent:**

* Rób review w miarę możliwości w ciągu 24 godzin, najpóźniej w ciągu 48 godzin; PR-y otwarte do wtorku 22:00 przejrzyj do środy 14:00. Czekający PR blokuje autora, więc review ma pierwszeństwo przed braniem nowego zadania.
* Jako zastępca nie czekaj na właściciela. Jeśli PR jest w twoim obszarze i masz czas, zrób review.
* Sprawdź: czy spełnia kryteria akceptacji, czy ma testy, czy jest czytelny, czy trzyma się ADR-ów i kontraktów.
* Komentarze pisz po angielsku i zaczynaj od prefiksu, żeby autor wiedział, jaką mają wagę:

  * `blocker:` trzeba poprawić przed merge'em,
  * `suggestion:` warto rozważyć, decyduje autor,
  * `question:` chcę zrozumieć,
  * `nit:` drobiazg stylistyczny, opcjonalny.
* Zatwierdzaj, gdy jest wystarczająco dobrze, a nie idealnie. Dalsze poprawki mogą trafić do nowych issue.

---

## 11. CODEOWNERS

Plik `.github/CODEOWNERS` przypisuje katalogi do obszarów. Przy każdym katalogu wpisujemy **właściciela i zastępcę**, więc akceptacja jednego z nich wystarcza, a właściciel nie musi zatwierdzać własnego PR-a.

```
# Strumień zdarzeń i czas rzeczywisty: Jakub, zastępca Kacper
/backend/libs/events/                 @jakub-jurkian @Kackor11>
/backend/services/tracking-service/   @jakub-jurkian @Kackor11>

# Frontend: Jakub, zastępca Tomasz
/frontend/                            @jakub-jurkian @TomaszDylik

# Dane i analityka: Kacper, zastępca Marcel
/backend/services/stats-service/      @Kackor11> @HolySsquirrell

# Integracja z ZTM: Marcel, zastępca Kacper
/backend/services/ingest-service/     @HolySsquirrell @Kackor11>
/fixtures/                            @HolySsquirrell @Kackor11>

# Infrastruktura i jakość: Marcel, zastępca Tomasz
/infra/                               @HolySsquirrell @TomaszDylik
/.github/                             @HolySsquirrell @TomaszDylik
/backend/build-logic/                 @HolySsquirrell @TomaszDylik

# Użytkownicy i bezpieczeństwo: Tomasz, zastępca Jakub
/backend/services/trips-service/      @TomaszDylik @jakub-jurkian
/infra/keycloak/                      @TomaszDylik @jakub-jurkian
```

Katalog `docs/` nie ma właściciela, więc dokumentację może zatwierdzić każdy.

---

## 12. Zmiany w tym procesie

Ten workflow to punkt startowy, a nie stałe prawo. co kilka tygodni sprawdzamy:

* ile trwa zadanie od wzięcia do merge'a,
* czy PR-y czekają na review i na kogo,
* czy któryś szablon albo zasada jest regularnie pomijana.

Co nie działa, upraszczamy albo usuwamy, a zmianę wpisujemy do tego pliku.

