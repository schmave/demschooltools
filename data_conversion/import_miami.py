import re

import psycopg2

ORG_ID = 13

conn = psycopg2.connect(
    "dbname=school_crm user=evan password=abc123 host=localhost port=5433"
)
cur = conn.cursor()
# cur.execute("set client_encoding to 'latin1'")


def import_lawbook():
    cur.execute(
        "DELETE from entry WHERE section_id in "
        "(SELECT s.id from section s join chapter c on s.chapter_id=c.id where organization_id=%d)"
        % ORG_ID
    )
    cur.execute(
        "DELETE from section WHERE chapter_id in "
        "(SELECT id from chapter where organization_id=%d)" % ORG_ID
    )
    cur.execute("DELETE from chapter WHERE organization_id=%d" % ORG_ID)
    chapter_id = None
    section_id = None
    rule_content = ""
    rule_num = ""
    rule_title = ""

    passed_intro = False

    cur.execute(
        """INSERT into chapter (num, title, organization_id)
        VALUES (%s, %s, %s) returning id""",
        ("A", "All Rules", ORG_ID),
    )
    chapter_id = int(cur.fetchone()[0])

    for line in open("lawbook.txt"):
        if "___" in line:
            passed_intro = True
        if not passed_intro or "___" in line:
            continue

        chapter_match = re.search(r"P(\d+):+(.*)", line)
        rule_match = re.search(r"(\d+)\. (.*): (.*)", line)

        if chapter_match or rule_match:
            if rule_num and rule_title and rule_content:
                # print('RULE', rule_num, rule_title, rule_content)
                cur.execute(
                    """INSERT into entry(num, title, section_id, content)
                    VALUES (%s, %s, %s, %s)""",
                    (rule_num, rule_title, section_id, rule_content),
                )
                rule_content = ""

        if chapter_match:
            num = "%02d" % int(chapter_match.group(1).strip())
            cur.execute(
                """INSERT into section(num, title, chapter_id) VALUES
                (%s, %s, %s) returning id""",
                (num, chapter_match.group(2).strip(), chapter_id),
            )
            section_id = int(cur.fetchone()[0])

            print(chapter_match.group(1), chapter_match.group(2), chapter_id)
            rule_content = ""
        elif rule_match:
            rule_num = rule_match.group(1)
            rule_title = rule_match.group(2).strip()
            rule_content = rule_match.group(3).strip()
            # rule_content = re.sub(r'\([-/0-9]+\)', '', rule_content).strip())

    print("Now manually copy over the preamble")


import_lawbook()
conn.commit()
cur.close()
conn.close()
