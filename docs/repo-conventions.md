# Konwencje i setup repozytorium: BusRadar Gdańsk

Ten dokument opisuje narzędzia, strukturę repozytorium i konwencje, z którymi zespół startuje w etapie 0. Na start wystarczy to minimum; resztę doprecyzowujemy na retrospektywach.

> **Status: propozycja.** Wersje narzędzi i strukturę katalogów zespół zatwierdza w ADR-000 po ustaleniu architektury.

## 1. Wersje i narzędzia

Backend budujemy na Javie 25 i Spring Boot 4.1 z Gradle, frontend na Angularze 22 i Node 24 LTS. Wersje są zapisane w repozytorium, żeby każdy pracował na tych samych.

| Narzędzie | Wersja | Uzasadnienie |
| --- | --- | --- |
| Java | 25 (LTS) | aktualne wydanie LTS, obsługiwane przez Spring Boot 4.1 |
| Spring Boot | 4.1.x | obecna stabilna linia (od czerwca 2026); 4.0 traci darmowe wsparcie z końcem 2026, czyli w trakcie projektu |
| Gradle | 9.x, Kotlin DSL (`build.gradle.kts`) | Gradle Wrapper w repozytorium, więc każdy używa tej samej wersji |
| Node.js | 24 LTS | Node 26 dopiero wchodzi w LTS, a Angular dodaje wsparcie nowego Node'a z opóźnieniem; podbijamy w kolejnych miesiącach |
| Angular | 22 (22.0.x) | aktualna stabilna wersja |

Dodatkowo:

- wersje bibliotek w jednym pliku `gradle/libs.versions.toml` (version catalog),
- Java toolchain w Gradle: `languageVersion = 25`,
- plik `.nvmrc` z wersją Node'a,
- formatowanie automatyczne: Spotless dla Javy, Prettier i ESLint dla Angulara, wspólny `.editorconfig`.

Wersje sprawdzone w październiku 2026: [Spring Boot](https://www.herodevs.com/blog-posts/spring-boot-versions-eol-dates-and-latest-releases-april-2026), [Node.js](https://endoflife.date/nodejs), [Angular](https://www.gitclear.com/open_repos/angular/angular/release/v22.0.6).

## 2. Struktura monorepo

Jedno repozytorium zawiera backend, frontend, infrastrukturę, dane testowe i dokumentację.

```text
busradar/
├── backend/
│   ├── settings.gradle.kts            # lista modułów
│   ├── build.gradle.kts
│   ├── gradle/libs.versions.toml      # wszystkie wersje w jednym miejscu
│   ├── build-logic/                   # wspólna konfiguracja (convention plugins)
│   ├── libs/
│   │   └── events/                    # rekordy zdarzeń + nazwy topików (kontrakt z ADR-001)
│   └── services/
│       ├── ingest-service/
│       ├── tracking-service/          # tracking + detektor przejazdów
│       ├── stats-service/
│       └── trips-service/
├── frontend/                          # workspace Angulara
├── infra/
│   ├── docker-compose.yml
│   ├── keycloak/realm-export.json
│   └── .env.example
├── fixtures/ztm/                      # nagrane dane z API ZTM
├── docs/
│   ├── adr/
│   └── ...                            # karta, model pracy, przebieg systemu
├── .github/
│   ├── workflows/
│   ├── ISSUE_TEMPLATE/
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── CODEOWNERS
├── .editorconfig
└── README.md
```

Zasady:

- **`build-logic`** trzyma wspólną konfigurację (Java 25, Spotless, JUnit), więc `build.gradle.kts` każdego serwisu ma kilka linijek zamiast czterech kopii tych samych ustawień.
- **`libs/events`** to jedyny wspólny kod. Między serwisami nie ma wspólnych „utils”, bo każdy serwis ma być niezależny.
- **Pakiety:** `pl.busradar.ingest`, `pl.busradar.tracking`, `pl.busradar.stats`, `pl.busradar.trips`.
- **`CODEOWNERS`** mapuje katalogi na właścicieli obszarów z dokumentu „Model pracy zespołu”.
- **README** pozwala uruchomić projekt trzema komendami: `git clone`, `docker compose up`, adres w przeglądarce. Haseł nie ma w repozytorium; przykładowe wartości są w `.env.example`.

## 3. Nazwy gałęzi i merge

Gałąź nazywamy według wzoru **`<typ>/<nr-issue>-<krótki-opis>`**: małe litery, myślniki, opis po angielsku.

| Typ | Kiedy | Przykład |
| --- | --- | --- |
| `feature/` | nowa funkcja | `feature/12-kafka-topics` |
| `fix/` | błąd | `fix/34-duplicate-positions` |
| `chore/` | konfiguracja, zależności, CI | `chore/5-ci-pipeline` |
| `docs/` | dokumentacja, ADR | `docs/8-adr-001-events` |
| `refactor/` | zmiana bez nowej funkcji | `refactor/41-stats-queries` |
| `test/` | same testy | `test/27-ingest-wiremock` |

- Typy gałęzi odpowiadają typom w Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`).
- Numer issue w nazwie pozwala GitHubowi połączyć pull request z zadaniem na tablicy.
- Gałęzie są krótkie: żyją od rozpoczęcia zadania do merge'a, potem je usuwamy.
- Merge do `main` robimy przez **squash**, więc historia `main` ma jeden commit na zadanie.

## 4. Etykiety na GitHubie

Każde zadanie ma jedną etykietę obszaru i jedną etykietę typu. Statusu (w toku, review) nie trzymamy w etykietach, od tego są kolumny tablicy.

### Obszar

Etykiety odpowiadają obszarom z dokumentu „Model pracy zespołu”.

| Etykieta | Obszar | Właściciel |
| --- | --- | --- |
| `area: streaming` | Strumień zdarzeń i czas rzeczywisty | Jakub Jurkian |
| `area: frontend` | Frontend | Jakub Jurkian |
| `area: data` | Dane i analityka | Kacper Korzekwa |
| `area: ztm` | Integracja z ZTM | Marcel Jaroszewicz |
| `area: infra` | Infrastruktura i jakość | Marcel Jaroszewicz |
| `area: auth` | Użytkownicy i bezpieczeństwo | Tomasz Dylik |

### Typ

| Etykieta | Kiedy |
| --- | --- |
| `type: feature` | nowa funkcja |
| `type: bug` | błąd |
| `type: chore` | konfiguracja, zależności, CI |
| `type: docs` | dokumentacja, ADR |
| `type: spike` | zadanie badawcze; wynikiem jest notatka albo ADR, a nie kod |

### Dodatkowe

- `blocked`: zadanie czeka na coś albo na kogoś.
- `needs-decision`: zadanie wymaga decyzji zespołu. Team lead przed każdą środą przegląda te zadania i wpisuje je do punktu „Decyzje” na spotkaniu.

## 5. Nagrana próbka danych z ZTM

Jedna nagrana godzina danych z API pozwala każdemu pisać i testować kod bez dostępu do API i bez czekania na ingest-service. Ten sam plik posłuży testom z WireMockiem, konsumentom Kafki i trybowi odtwarzania.

- **Co:** odpowiedzi `gpsPositions` co 20 s przez ok. godzinę w porannym szczycie dnia roboczego (np. 7:00–8:00) oraz jednorazowo lista przystanków.
- **Format:** JSON Lines, jeden snapshot na linię, skompresowany gzipem, np. `fixtures/ztm/2026-10-27-morning.jsonl.gz`.
- **Rozmiar:** ok. 400 pojazdów po ok. 200 B daje ok. 80 KB na snapshot, czyli ok. 14 MB na godzinę bez kompresji i 1–2 MB po kompresji.
- **Jak:** prosty skrypt (curl w pętli albo mała klasa w Javie) uruchomiony raz; obszar Integracja z ZTM.
- **Licencja:** jeśli repozytorium jest publiczne, przed dodaniem danych sprawdzamy licencję i podajemy źródło danych w README.

## 6. Health checki

Serwisy startują dopiero wtedy, gdy Kafka, PostgreSQL i Redis są gotowe. Bez tego pierwsze uruchomienie kończy się błędami połączeń i „u mnie nie działa”.

- Każdy serwis Spring Boot ma `spring-boot-starter-actuator`, który udostępnia endpoint `/actuator/health`.
- Infrastruktura w `docker-compose.yml` ma `healthcheck`, a serwisy czekają na `condition: service_healthy`.

```yaml
services:
  postgres:
    image: postgres:17
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U busradar"]
      interval: 5s
      retries: 10

  kafka:
    image: apache/kafka:4.1.0
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1"]
      interval: 10s
      retries: 10

  redis:
    image: redis:8
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s

  tracking-service:
    build: ../backend/services/tracking-service
    depends_on:
      kafka:
        condition: service_healthy
      redis:
        condition: service_healthy
```

Keycloak ma health checki po ustawieniu `KC_HEALTH_ENABLED=true` (osobny port 9000), ale w jego obrazie nie ma `curl`, więc test trzeba zbudować inaczej, np. przez bash. To zadanie obszaru Użytkownicy i bezpieczeństwo przy konfiguracji realmu.

## 7. ADR

Kluczowe decyzje zapisujemy jako krótkie dokumenty ADR w katalogu `docs/adr/`, w plikach `NNNN-tytul.md`. Status zmienia się na „zaakceptowany” na środowym spotkaniu. Zaakceptowanego ADR nie edytujemy: zmiana decyzji to nowy ADR, który zastępuje stary.

### Szablon: `docs/adr/0000-template.md`

```markdown
# ADR-NNN: Tytuł decyzji

- Status: proponowany | zaakceptowany | zastąpiony przez ADR-XXX
- Data: RRRR-MM-DD
- Autor: imię (obszar)

## Kontekst
Jaki problem rozwiązujemy i jakie mamy ograniczenia.

## Decyzja
Co wybieramy, w 2–5 zdaniach.

## Rozważane alternatywy
- Opcja A: zalety, wady
- Opcja B: zalety, wady

## Konsekwencje
Co ta decyzja ułatwia, co utrudnia, co trzeba będzie zrobić.
```

### Pierwsze ADR-y

| Nr | Tytuł | Obszar | Termin |
| --- | --- | --- | --- |
| ADR-000 | Struktura repozytorium i narzędzia: monorepo, Gradle z Kotlin DSL, Java 25, Spring Boot 4.1, Angular 22, Node 24 | Infrastruktura i jakość | po ustaleniu architektury |
| ADR-001 | Topiki Kafki i format zdarzeń: `vehicle-positions`, `stop-passages`, klucze, partycje, retencja, dead-letter topic | Strumień zdarzeń | etap 0 |
| ADR-002 | Dlaczego Kafka: alternatywy RabbitMQ, Redis Streams, PostgreSQL jako kolejka | Strumień zdarzeń | etap 0–1 |
| ADR-003 | Wykrywanie przejazdów przez przystanki: promień, odczyty przed i po przystanku, pojazd stojący na pętli | Strumień zdarzeń, z Dane i analityka | etap 1 |
| ADR-004 | Schemat bazy: partycje dzienne, retencja 14 dni, tabela `stop_passages` | Dane i analityka | etap 1 |
