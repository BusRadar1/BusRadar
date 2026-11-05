# Workflow na przykładzie: jedno zadanie od pomysłu do `main`

Ten dokument pokazuje cały przebieg jednego zadania, krok po kroku, na konkretnym przykładzie. Zasady, na których się opiera, są w `CONTRIBUTING.md`.

**Przykład:** tracking-service ma czytać topic `vehicle-positions` i zapisywać ostatnią pozycję każdego pojazdu w Redisie.

* **Autor:** Jakub (obszar Strumień zdarzeń i czas rzeczywisty)
* **Recenzent:** Kacper, zastępca w tym obszarze. Autor jest właścicielem obszaru, więc review robi zastępca.
* **Tydzień:** zadanie wzięte w środę na spotkaniu, gotowe w sobotę, merge w niedzielę.

```
śr         czw ──── sob      sob        nd              śr
spotkanie  praca     PR       review     merge           demo na
(bierze)             (gotowe) (≤ 24 h)   (karta: Done)   spotkaniu
```

PR otwieramy, gdy tylko zadanie jest gotowe. Wtorek 22:00 (PR) i środa 14:00 (review) to ostateczne terminy na wypadek, gdy praca zajmie cały tydzień, a nie jedyny moment na PR.

---

## Krok 1. Założenie issue

**Kto:** ktokolwiek, zwykle właściciel obszaru. **Kolumna:** Backlog.

Na GitHubie: **Issues → New issue → Feature**. Wypełniony szablon:

```markdown
Title: feat(tracking): store latest vehicle position in Redis

## Description
As a passenger, I want the live map to show where each vehicle is right now,
so tracking-service must keep the latest known position of every vehicle.

## Acceptance Criteria
- [ ] tracking-service consumes `vehicle-positions` with consumer group `tracking`
- [ ] the latest position per vehicle is stored in Redis under key `vehicle:{vehicleId}`
- [ ] an older reading (earlier `readAt`) does not overwrite a newer one
- [ ] integration test with Testcontainers (Kafka + Redis) covers both cases

## Notes
- Event format: see ADR-001 and `backend/libs/events/VehiclePositionEvent`
- Use the recorded sample `fixtures/ztm/2026-10-27-morning.jsonl.gz` for test data
- Dependencies: none, uses recorded data instead of ingest-service
- Size: L (4.5-6h)
```

Etykiety: `area: streaming`, `type: feature`. GitHub nadaje numer, w przykładzie **#14**. Issue trafia na tablicę do kolumny **Backlog**.

---

## Krok 2. Doprecyzowanie i przeniesienie do To Do

**Kto:** właściciel obszaru. **Kolumna:** Backlog → **To Do**.

Właściciel sprawdza Definition of Ready:

* [x] opis mówi, co i dlaczego,
* [x] kryteria akceptacji da się sprawdzić,
* [x] ma etykiety `area:` i `type:`,
* [x] nie zależy od niezrobionej pracy, bo zamiast ingest-service używa nagranych danych,
* [x] mieści się w 6 godzinach pracy.

Wszystko się zgadza, więc przesuwa kartę do **To Do**. Jeśli czegoś brakuje, dopisuje to albo zadaje pytanie w komentarzu.

---

## Krok 3. Wzięcie zadania

**Kto:** autor. **Kolumna:** To Do → **In Progress** (automatycznie). **Kiedy:** na środowym spotkaniu albo dowolnie w tygodniu.

1. Na issue: **Assignees → przypisz siebie**. Karta sama przechodzi z **To Do** do **In Progress**, więc nie przesuwamy jej ręcznie.
2. Sprawdź limit: masz najwyżej 2 zadania w toku.
3. Utwórz gałąź od aktualnego `main`:

```bash
git switch main
git pull
git switch -c feature/14-tracking-redis-latest-position
```

---

## Krok 4. Praca nad zadaniem

**Kto:** autor. **Kolumna:** In Progress. **Kiedy:** w dowolnym czasie w tygodniu.

Pracuj małymi krokami i commituj po każdym sensownym kawałku:

```bash
git add .
git commit -m "feat(tracking): add kafka listener for vehicle-positions"

git add .
git commit -m "feat(tracking): store latest position per vehicle in redis"

git add .
git commit -m "test(tracking): cover out-of-order readings with testcontainers"
```

Przed wypchnięciem sprawdź lokalnie, że wszystko się buduje i testy przechodzą:

```bash
cd backend
./gradlew :services:tracking-service:build

cd ../infra
docker compose up
```

Wypychaj wcześnie, nawet niedokończoną pracę. To twoja gałąź, nikomu nie przeszkadza:

```bash
git push -u origin feature/14-tracking-redis-latest-position
```

**Jeśli utkniesz:** etykieta `blocked` na issue i wiadomość na `#pomoc`. **Jeśli nie zdążysz:** informacja na Discordzie najpóźniej w niedzielę wieczorem.

### Aktualizacja gałęzi

Jeśli w tym czasie ktoś scalił coś do `main`, zaktualizuj swoją gałąź przed otwarciem PR-a:

```bash
git fetch origin
git rebase origin/main
# rozwiąż ewentualne konflikty, potem:
git push --force-with-lease
```

`--force-with-lease` nadpisuje tylko twoją gałąź i odmówi, jeśli ktoś inny coś do niej wypchnął. Na `main` nigdy nie używamy force push.

---

## Krok 5. Otwarcie Pull Requesta

**Kto:** autor. **Kolumna:** In Progress → **In Review** (automatycznie). **Kiedy:** gdy tylko zadanie jest gotowe; najpóźniej we wtorek do 22:00.

Na GitHubie: **Compare & pull request** (albo `gh pr create`). Tytuł w formacie Conventional Commits, bo po squash merge stanie się commitem na `main`. Wypełniony szablon:

```markdown
Title: feat(tracking): store latest vehicle position in Redis

Closes #14

## What Changed
- `VehiclePositionListener` consumes `vehicle-positions` (group `tracking`)
- `LatestPositionRepository` saves the position in Redis only if `readAt` is newer
  than the stored one (Lua script, atomic compare-and-set)
- Integration test with Testcontainers: Kafka + Redis, in-order and out-of-order readings

## How to Test
1. `cd infra && docker compose up`
2. Replay the sample: `./scripts/replay.sh fixtures/ztm/2026-10-27-morning.jsonl.gz`
3. `docker compose exec redis redis-cli GET vehicle:1234` returns the latest position

## Checklist
- [x] Tests added or updated, or not needed
- [x] No secrets, tokens or `.env` values committed

## AI Usage
Claude helped write the Lua script for the atomic compare-and-set; reviewed and
tested manually.

## Notes for the Reviewer
Please look closely at the Lua script in `LatestPositionRepository`. I chose it
over a Redis transaction to avoid a race between two instances.
```

Karta sama przechodzi do **In Review**, gdy PR jest otwarty i ma `Closes #14`. Przy PR-ze otwartym jako *draft* karta zostaje w **In Progress**, dopóki nie klikniesz **Ready for review**. Wpisz też na karcie godziny pracy w polu **Spent time (h)**. GitHub sam poprosi o review właścicieli katalogu z pliku `CODEOWNERS`.

> W `CODEOWNERS` przy każdym katalogu wpisani są właściciel **i** zastępca (CONTRIBUTING.md, sekcja 11). Akceptacja jednego z nich wystarczy, więc właściciel nie musi zatwierdzać własnego PR-a.

---

## Krok 6. CI

**Kto:** automat. **Kolumna:** In Review.

Po otwarciu PR-a GitHub Actions buduje projekt i uruchamia testy. Wynik widać na dole PR-a:

* ✅ zielone: można przejść do review,
* ❌ czerwone: autor poprawia i wypycha zmiany; CI uruchamia się ponownie samo.

Bez zielonego CI przycisk merge jest zablokowany przez ochronę gałęzi `main`.

---

## Krok 7. Review

**Kto:** recenzent (tu Kacper). **Kolumna:** In Review. **Kiedy:** w miarę możliwości w ciągu 24 godzin, najpóźniej w ciągu 48 godzin albo do środy 14:00.

Review ma pierwszeństwo przed braniem nowego zadania, bo czekający PR blokuje autora. Dla zadań `docs` i `chore` review może zrobić dowolna osoba z zespołu.

Recenzent sprawdza kryteria akceptacji, testy, czytelność i zgodność z ADR-001. Komentarze pisze po angielsku, z prefiksem:

```
blocker: If Redis is down, the listener throws and the message is retried forever.
Can we route it to the DLT after 3 attempts, as described in ADR-001?

suggestion: Consider extracting the Redis key format to a constant in `events`,
stats-service will need it too.

nit: Typo in the test name: `shouldIgnoreOlderReadnig`.
```

Na koniec wybiera jedną z opcji:

* **Request changes**, gdy jest choć jeden `blocker:`,
* **Approve**, gdy zostały tylko `suggestion:` i `nit:`, które autor może poprawić albo zostawić,
* **Comment**, gdy ma tylko pytania.

---

## Krok 8. Poprawki po review

**Kto:** autor. **Kolumna:** In Review.

Autor odpowiada na każdy komentarz i dopisuje poprawki jako nowe commity na tej samej gałęzi:

```bash
git add .
git commit -m "fix(tracking): send failed messages to DLT after 3 retries"
git push
```

Odpowiedzi w PR-ze:

```
> blocker: If Redis is down...
Done in a1b2c3d: DefaultErrorHandler with 3 retries, then DLT.

> suggestion: Consider extracting the Redis key format...
Good idea, but stats-service does not need it yet. Created #21 to do it later.

> nit: Typo in the test name
Fixed.
```

Recenzent sprawdza poprawki i daje **Approve**. Po review wpisuje na karcie swoje godziny w polu **Review time (h)**.

---

## Krok 9. Merge

**Kto:** autor. **Kolumna:** In Review → **Done** (automatycznie).

Gdy PR ma akceptację i zielone CI:

1. **Squash and merge**: wszystkie commity z gałęzi łączą się w jeden commit na `main` o tytule PR-a:
`feat(tracking): store latest vehicle position in Redis (#15)`
2. **Delete branch** na GitHubie.
3. Dzięki `Closes #14` issue zamyka się samo, a tablica przesuwa kartę do **Done**.

Po stronie lokalnej:

```bash
git switch main
git pull
git branch -d feature/14-tracking-redis-latest-position
```

---

## Krok 10. Środowe spotkanie

**Kto:** autor. **Kolumna:** Done.

W punkcie „Stan prac” autor pokazuje efekt w 1–2 minuty, np. w `redis-cli` albo na mapie, jeśli frontend już korzysta z tych danych. Potem bierze kolejne zadanie z **To Do** i cykl zaczyna się od kroku 3.

---

## Ściąga

|Krok|Kto|Kolumna|Najważniejsze|
|-|-|-|-|
|1. Issue|każdy|Backlog|szablon, etykiety `area:` + `type:`|
|2. Doprecyzowanie|właściciel obszaru|→ To Do|Definition of Ready|
|3. Wzięcie|autor|→ In Progress (automat)|przypisz się do issue, max 2 zadania, gałąź `typ/nr-opis`|
|4. Praca|autor|In Progress|małe commity, `push` wcześnie, `blocked` gdy trzeba|
|5. PR|autor|→ In Review (automat)|gdy gotowe (najpóźniej wt. 22:00), `Closes #nr`, tytuł jak commit, wpisz Spent time|
|6. CI|automat|In Review|musi być zielone|
|7. Review|właściciel albo zastępca (`docs`/`chore`: każdy)|In Review|w ciągu 24 h (najpóźniej 48 h / śr. 14:00), prefiksy komentarzy, wpisz Review time|
|8. Poprawki|autor|In Review|odpowiedź na każdy komentarz|
|9. Merge|autor|→ Done|squash, usuń gałąź, issue zamyka się samo|
|10. Demo|autor|Done|pokaż na środowym spotkaniu|

---

## Jednorazowa konfiguracja tablicy

Konfigurację robi raz obszar Infrastruktura i jakość.

**1. Wbudowane automatyzacje** GitHub Projects (**Project → ⋯ → Workflows**):

* **Item closed** → status **Done**,
* **Pull request merged** → status **Done**,
* **Auto-add to project** dla nowych issue z repozytorium → status **Backlog**.

**2. Workflow GitHub Actions** `.github/workflows/project-status.yml` (do dodania; na przypisanie osoby i otwarcie PR-a nie ma wbudowanej automatyzacji):

* `issues: assigned`: jeśli karta jest w **To Do**, zmienia status na **In Progress**. Karty z **Backlog** nie rusza, żeby nie ominąć *Definition of Ready*.
* `pull_request: opened, reopened, ready_for_review`: dla PR-a, który nie jest draftem, zmienia status issue z `Closes #nr` na **In Review**.
* Potrzebuje tokenu z dostępem do projektu (PAT z zakresem `project` albo GitHub App) zapisanego w sekretach repozytorium jako `PROJECT_TOKEN`, bo domyślny `GITHUB_TOKEN` nie zapisuje do GitHub Projects.

**Ręcznie zostaje** przesunięcie z **Backlog** do **To Do** (właściciel obszaru) oraz wpisanie godzin w polach **Spent time (h)** i **Review time (h)**.

