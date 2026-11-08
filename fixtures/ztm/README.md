# ZTM recordings

Recorded responses of the ZTM Gdańsk API. They let the team develop and test with real data without calling the live API. The replay mode reads the same format.

## File format

- One file per hour, named `YYYY-MM-DD-HH.jsonl.gz`. The hour is in **UTC** (Gdańsk is UTC+1 in winter and UTC+2 in summer).
- gzip-compressed JSON Lines: one line per API call.
- Each line: `{"fetchedAt": "2026-11-09T06:00:20Z", "body": <whole API response>}`.
- `body` is the response of `https://ckan2.multimediagdansk.pl/gpsPositions?v=2`: an object with `lastUpdate` and a `vehicles` list (position, speed, direction, line, trip, delay in seconds).

## Record new data

Python 3 is enough, nothing to install:

```bash
python scripts/record_ztm.py                # saves to fixtures/ztm
python scripts/record_ztm.py my_folder      # saves to my_folder
```

The folder can also be set with the environment variable `ZTM_OUTPUT_DIR` (a folder given on the command line wins). The script downloads the positions every 20 s, logs a failed request and keeps running. Stop it with Ctrl+C.

## Read a file

```python
import gzip, json

with gzip.open("fixtures/ztm/<file>.jsonl.gz", "rt", encoding="utf-8") as file:
    for line in file:
        record = json.loads(line)
        print(record["fetchedAt"], len(record["body"]["vehicles"]))
```

In a shell: `gzip -dc fixtures/ztm/<file>.jsonl.gz | head -n 1`.

## Source and licence

Data: Zarząd Transportu Miejskiego w Gdańsku, published on the [Otwarty Gdańsk](https://ckan.multimediagdansk.pl/dataset/tristar/resource/0683c92f-7241-4698-bbcc-e348ee355076) portal (resource "Pozycja GPS pojazdów"), licence [Creative Commons Attribution](http://www.opendefinition.org/licenses/cc-by). Terms of use: "Regulamin korzystania z danych" linked on the portal.
