# Korzystanie z AI w projekcie

Zespół korzysta z narzędzi AI (tj. Claude czy Gemini) przy kodzie, dokumentacji i projektowaniu.

---

## Do czego używamy AI

* pomoc w pisaniu i refaktoryzacji kodu, testów i konfiguracji,
* wyjaśnianie technologii (Kafka, Angular, Spring) i komunikatów błędów,
* szkice dokumentacji, ADR i opisów Pull Requestów,
* przegląd własnego kodu, zanim wystawimy PR.

## Do czego nie używamy AI

* do podejmowania decyzji architektonicznych za zespół: AI może zaproponować rozwiązanie, ale decyzja zapada na spotkaniu i trafia do ADR,
* do zatwierdzania PR-ów: review robi zawsze człowiek,
* do generowania kodu, którego autor nie rozumie.

---

## Zasady

1. **Autor odpowiada za kod.** Każdy fragment wygenerowany przez AI autor rozumie i potrafi wyjaśnić na review i na prezentacji. Kodu, którego nie rozumiemy, nie wysyłamy.
2. **Review bez wyjątków.** Kod napisany z pomocą AI przechodzi ten sam review i te same testy w CI co każdy inny.
3. **Jawność w Pull Requestach.** Szablon PR ma sekcję **AI Usage**. Wpisujemy „None” albo krótko, do czego użyliśmy AI, np. *„Claude generated the Kafka consumer config; reviewed and adjusted manually.”*
4. **Jawność w commitach.** Commity utworzone przez Claude Code zostawiamy z linijką `Co-Authored-By`, którą narzędzie dodaje automatycznie. Nie usuwamy jej.
5. **Bez sekretów.** Do AI nie wklejamy haseł, tokenów, kluczy API, zawartości pliku `.env` ani danych osobowych.
6. **Sprawdzamy fakty.** AI potrafi się mylić, zwłaszcza w wersjach bibliotek i konfiguracji. Wersje, nazwy opcji i API sprawdzamy w oficjalnej dokumentacji.

---

## Kontekst dla AI (plik) - do uzgodnienia

...

