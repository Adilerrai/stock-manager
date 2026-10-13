import csv
import sys

print('=== VERIFICATION SECTIONS.CSV ===')
sections = {}
try:
    with open('sections.csv', mode='rb') as raw:
        has_bom = raw.read(3) == b'\xef\xbb\xbf'
        print(f'sections.csv contient un UTF-8 BOM : {has_bom}')
    with open('sections.csv', mode='r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        print(f'Colonnes sections.csv : {reader.fieldnames}')
        for i, row in enumerate(reader, 1):
            sections[row['section']] = row['name']
    print(f'-> Total sections : {len(sections)} (Sections I a XXI bien presentes)')
except Exception as e:
    print(f'Erreur lecture sections.csv: {e}')
    sys.exit(1)

print('\n=== VERIFICATION HARMONIZED-SYSTEM.CSV ===')
levels_count = {}
errors = []
missing_sections = set()
total_rows = 0
codes = set()
duplicates = []
empty_descriptions = 0

try:
    with open('harmonized-system.csv', mode='rb') as raw:
        has_bom_hs = raw.read(3) == b'\xef\xbb\xbf'
        print(f'harmonized-system.csv contient un UTF-8 BOM : {has_bom_hs}')
    with open('harmonized-system.csv', mode='r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        print(f'Colonnes harmonized-system.csv : {reader.fieldnames}')
        for i, row in enumerate(reader, start=2):
            total_rows += 1
            lvl = row.get('level')
            levels_count[lvl] = levels_count.get(lvl, 0) + 1
            sec = row.get('section')
            if sec not in sections:
                missing_sections.add(sec)
            
            code = row.get('hscode', '').strip()
            if not code:
                errors.append(f'Ligne {i}: hscode vide')
            elif code in codes:
                duplicates.append((i, code))
            else:
                codes.add(code)
                
            desc = row.get('description', '').strip()
            if not desc:
                empty_descriptions += 1
                
            if lvl and code and len(code) != int(lvl):
                errors.append(f'Ligne {i}: hscode \"{code}\" (longueur {len(code)}) different de level {lvl}')

    print(f'-> Total lignes enregistrees : {total_rows}')
    print(f'-> Repartition par niveau (level) : {dict(sorted(levels_count.items()))}')
    print(f'-> Codes uniques : {len(codes)}')
    print(f'-> Doublons trouves : {len(duplicates)}')
    if duplicates:
        print(f'   Exemples de doublons : {duplicates[:5]}')
    print(f'-> Descriptions vides : {empty_descriptions}')
    print(f'-> Sections dans harmonized-system non presentes dans sections.csv : {missing_sections if missing_sections else "Aucune (100% matche)"}')
    print(f'-> Incoherences longueur / level : {len(errors)}')
    if errors:
        print(f'   Exemples : {errors[:5]}')

except Exception as e:
    print(f'Erreur lecture harmonized-system.csv: {e}')
    sys.exit(1)
