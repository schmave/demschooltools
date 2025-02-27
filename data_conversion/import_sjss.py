import os
import re

import psycopg2

ORG_ID = 18

conn = psycopg2.connect(
    f"dbname=school_crm user={os.environ['DB_USER']} "
    f"password={os.environ['DB_PASSWORD']} host=localhost port=5433"
)
cur = conn.cursor()


def import_lawbook():
    cur.execute(
        "DELETE from manual_change WHERE entry_id in "
        "(SELECT s.id from section s join chapter c on s.chapter_id=c.id where organization_id=%d)"
        % ORG_ID
    )
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

    for line in open("lawbook.txt"):
        chapter_match = re.match(r"([VI]+)\.(.*)", line)
        rule_match = re.match(r"([A-Z]+)\.(.*)", line)

        if chapter_match or rule_match:
            if chapter_id and rule_num and rule_content:
                rule_title = " ".join(rule_content.split(" ")[:6])
                cur.execute(
                    """INSERT into entry(num, title, section_id, content)
                    VALUES (%s, %s, %s, %s)""",
                    (
                        rule_num.strip(),
                        rule_title,
                        section_id,
                        rule_content.strip(),
                    ),
                )
                # print("RULE", rule_num, rule_title)
                rule_content = ""

        if chapter_match and len(chapter_match.group(2).strip().split(" ")) < 5:
            print("CHAPTER", chapter_match.group(1), chapter_match.group(2))
            cur.execute(
                """INSERT into chapter (num, title, organization_id)
                VALUES (%s, %s, %s) returning id""",
                (
                    chapter_match.group(1).strip(),
                    chapter_match.group(2).strip(),
                    ORG_ID,
                ),
            )
            chapter_id = int(cur.fetchone()[0])
            cur.execute(
                """INSERT into section(num, title, chapter_id) VALUES
                (%s, %s, %s) returning id""",
                ("", "All rules", chapter_id),
            )
            section_id = int(cur.fetchone()[0])
            rule_content = ""
            rule_num = ""
        elif rule_match:
            rule_num = rule_match.group(1).strip()
            rule_content = rule_match.group(2).strip()
            # print("RULE", rule_num, rule_content)
        elif rule_num:
            rule_content += line


import_lawbook()
conn.commit()
cur.close()
conn.close()
