"""Pobiera Issues z BusRadar1/BusRadar przez GitHub API i zapisuje je do zadania_github.xlsx.

Arkusz "Zadania": ID, TYTUŁ, STATUS, ASSIGNEE, DATA, SPENT TIME (h).
Arkusz "Podsumowanie": formuły sumujące spent time i liczbę zadań na osobę.

Spent time i status z tablicy projektu wymagają uprawnienia read:project:
    gh auth refresh -s read:project
Bez niego skrypt działa, ale kolumna SPENT TIME jest pusta, a STATUS to open/closed.
"""
import json
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

from openpyxl import Workbook
from openpyxl.formatting.rule import FormulaRule
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side

OWNER, NAME = "BusRadar1", "BusRadar"
OUT = Path(__file__).with_name("zadania_github.xlsx")
SPENT_FIELD = "Spent time (h)"  # nazwa pola typu Number na tablicy projektu
STATUS_FIELD = "Status"          # kolumny tablicy
TEAM = ["jakub-jurkian", "Kackor11", "TomaszDylik", "HolySsquirrell"]

# Statusy do kolorowania: zielony = otwarte, szary = zakończone, reszta (np. "W toku", "Review") = żółty
GREEN_STATUSES = ["open", "Backlog", "To Do", "Gotowe do wzięcia"]
GREY_STATUSES = ["closed", "Done", "Zrobione"]

QUERY = """
query($owner: String!, $name: String!, $cursor: String) {
  repository(owner: $owner, name: $name) {
    issues(first: 100, after: $cursor, orderBy: {field: CREATED_AT, direction: ASC}) {
      pageInfo { hasNextPage endCursor }
      nodes {
        number title state createdAt
        assignees(first: 10) { nodes { login } }
        %s
      }
    }
  }
}
"""
PROJECT_FIELDS = """
        projectItems(first: 5) { nodes { fieldValues(first: 20) { nodes {
          ... on ProjectV2ItemFieldNumberValue { number field { ... on ProjectV2FieldCommon { name } } }
          ... on ProjectV2ItemFieldSingleSelectValue { name field { ... on ProjectV2FieldCommon { name } } }
        } } } }
"""


def get_token():
    try:
        return subprocess.run(["gh", "auth", "token"], capture_output=True, text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        sys.exit("Brak tokenu: zaloguj się poleceniem `gh auth login`.")


def graphql(token, query, cursor):
    body = json.dumps({"query": query, "variables": {"owner": OWNER, "name": NAME, "cursor": cursor}}).encode()
    req = urllib.request.Request("https://api.github.com/graphql", data=body, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req) as resp:
        return json.load(resp)


def fetch_issues(token, with_projects):
    query = QUERY % (PROJECT_FIELDS if with_projects else "")
    issues, cursor = [], None
    while True:
        data = graphql(token, query, cursor)
        if "errors" in data:
            raise RuntimeError(data["errors"][0]["message"])
        page = data["data"]["repository"]["issues"]
        issues += page["nodes"]
        if not page["pageInfo"]["hasNextPage"]:
            return issues
        cursor = page["pageInfo"]["endCursor"]


def project_values(issue):
    """Zwraca (status z tablicy, spent time) z pól projektu; None, jeśli brak."""
    status = spent = None
    for item in issue.get("projectItems", {}).get("nodes", []):
        for v in item["fieldValues"]["nodes"]:
            field = (v.get("field") or {}).get("name")
            if field == STATUS_FIELD and "name" in v:
                status = v["name"]
            elif field == SPENT_FIELD and "number" in v:
                spent = v["number"]
    return status, spent


def build_workbook(issues):
    wb = Workbook()
    ws = wb.active
    ws.title = "Zadania"
    headers = ["ID", "TYTUŁ", "STATUS", "ASSIGNEE", "DATA", "SPENT TIME (h)"]
    ws.append(headers)

    for i in issues:
        board_status, spent = project_values(i)
        status = board_status or i["state"].lower()
        if i["state"] == "CLOSED":
            status = "closed"
        assignees = ", ".join(a["login"] for a in i["assignees"]["nodes"])
        ws.append([i["number"], i["title"], status, assignees, i["createdAt"][:10], spent])

    last = ws.max_row
    thin = Side(style="thin", color="999999")
    border = Border(left=thin, right=thin, top=thin, bottom=thin)
    header_fill = PatternFill("solid", fgColor="1F3864")
    for cell in ws[1]:
        cell.font = Font(bold=True, color="FFFFFF")
        cell.fill = header_fill
        cell.alignment = Alignment(horizontal="center", vertical="center")
    for row in ws.iter_rows(min_row=1, max_row=last, max_col=len(headers)):
        for cell in row:
            cell.border = border
            if cell.row > 1:
                cell.alignment = Alignment(vertical="center", wrap_text=(cell.column == 2),
                                           horizontal="center" if cell.column != 2 else None)
    for row in range(2, last + 1):
        ws.cell(row, 6).number_format = "0.0"
    for col, width in zip("ABCDEF", (8, 70, 20, 24, 12, 16)):
        ws.column_dimensions[col].width = width
    ws.freeze_panes = "A2"
    ws.auto_filter.ref = f"A1:F{last}"

    # Kolory statusów jako formatowanie warunkowe: zmieniają się po ręcznej edycji komórki
    rng = f"C2:C{last}"
    def any_of(values):
        return "OR(" + ",".join(f'$C2="{v}"' for v in values) + ")"
    green = PatternFill("solid", bgColor="C6EFCE")
    grey = PatternFill("solid", bgColor="D9D9D9")
    yellow = PatternFill("solid", bgColor="FFEB9C")
    ws.conditional_formatting.add(rng, FormulaRule(formula=[any_of(GREEN_STATUSES)], fill=green, stopIfTrue=True))
    ws.conditional_formatting.add(rng, FormulaRule(formula=[any_of(GREY_STATUSES)], fill=grey, stopIfTrue=True))
    ws.conditional_formatting.add(rng, FormulaRule(formula=['$C2<>""'], fill=yellow))

    # Podsumowanie na osobę po prawej (kolumny H-J), formuły przeliczają się po wpisaniu godzin
    people = TEAM + sorted({a["login"] for i in issues for a in i["assignees"]["nodes"]} - set(TEAM))
    total = len(people) + 2
    for col, text in zip((8, 9, 10, 11), ("OSOBA", "LICZBA ZADAŃ", "ZAMKNIĘTE", "SPENT TIME (h)")):
        ws.cell(1, col, text)
    for n, person in enumerate(people, start=2):
        who = f'$D$2:$D${last},"*"&H{n}&"*"'
        closed = "+".join(f'COUNTIFS({who},$C$2:$C${last},"{s}")' for s in GREY_STATUSES)
        ws.cell(n, 8, person)
        ws.cell(n, 9, f"=COUNTIF({who})")
        ws.cell(n, 10, f"={closed}")
        ws.cell(n, 11, f'=SUMIF({who},$F$2:$F${last})')
    ws.cell(total, 8, "RAZEM")
    for col, letter in ((9, "I"), (10, "J"), (11, "K")):
        ws.cell(total, col, f"=SUM({letter}2:{letter}{total - 1})")
    for row in ws.iter_rows(min_row=1, max_row=total, min_col=8, max_col=11):
        for cell in row:
            cell.border = border
            cell.alignment = Alignment(horizontal="left" if cell.column == 8 else "center")
            if cell.row == 1:
                cell.font = Font(bold=True, color="FFFFFF")
                cell.fill = header_fill
            elif cell.row == total:
                cell.font = Font(bold=True)
                cell.fill = PatternFill("solid", fgColor="DDEBF7")
            if cell.column == 11 and cell.row > 1:
                cell.number_format = "0.0"
    for col, width in zip("GHIJK", (4, 24, 16, 14, 18)):
        ws.column_dimensions[col].width = width
    return wb


def main():
    sys.stderr.reconfigure(encoding="utf-8")
    sys.stdout.reconfigure(encoding="utf-8")  # konsola Windows (cp1252) nie ma polskich znaków
    token = get_token()
    try:
        issues = fetch_issues(token, with_projects=True)
    except (RuntimeError, urllib.error.HTTPError) as e:
        print(f"Uwaga: bez danych z tablicy projektu ({e}).\n"
              "Aby pobrać Spent time i status z tablicy: gh auth refresh -s read:project")
        issues = fetch_issues(token, with_projects=False)
    out = OUT
    try:
        build_workbook(issues).save(out)
    except PermissionError:  # plik otwarty w Excelu: zapisz obok pod nazwą z godziną
        out = OUT.with_name(f"{OUT.stem}_{datetime.now():%Y-%m-%d_%H%M%S}.xlsx")
        build_workbook(issues).save(out)
        print(f"{OUT.name} jest otwarty w Excelu, zapisuję jako {out.name}")
    print(f"Zapisano {len(issues)} zadań do {out}")


if __name__ == "__main__":
    main()
