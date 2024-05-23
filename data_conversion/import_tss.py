import re

import psycopg2

ORG_ID = 11

conn = psycopg2.connect("dbname=school_crm user=evan host=localhost port=5432")
cur = conn.cursor()


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
    rule_num = None
    rule_title = ""
    is_rule_sublist = False
    last_subrule_num = None

    i = 0
    for line in open("lawbook.txt", "rb"):
        line = line.decode("utf-8")
        if "___" in line:
            pass

        chapter_match = re.match(r"([A-Z])\. (.*)", line)
        is_chapter = bool(chapter_match) or line.strip() == "Preamble"

        rule_match = re.match(r" *(\d+)\. *(.*)", line)
        is_new_rule = False

        if rule_match:
            new_rule_num = int(rule_match.group(1))
            if last_subrule_num is not None and new_rule_num <= last_subrule_num:
                is_rule_sublist = not is_rule_sublist
            is_new_rule = not is_rule_sublist
            last_subrule_num = new_rule_num

        if is_chapter or is_new_rule:
            if rule_num and rule_title and rule_content:
                cur.execute(
                    """INSERT into entry(num, title, section_id, content)
                    VALUES (%s, %s, %s, %s)""",
                    ("%02d" % rule_num, rule_title, section_id, rule_content),
                )
                print("RULE", rule_num, rule_title, rule_content)
                rule_content = ""

        if rule_match:
            if is_new_rule:
                rule_content = rule_match.group(2)
                rule_title = rule_content[:30] + (
                    "..." if len(rule_content) > 30 else ""
                )
                rule_num = new_rule_num
            else:
                rule_content += "\n" + line.strip()
        elif is_chapter:
            if chapter_match:
                num = chapter_match.group(1).strip()
                title = chapter_match.group(2).strip()
            else:
                num = "0"
                title = "Preamble"

            cur.execute(
                """INSERT into chapter (num, title, organization_id)
                VALUES (%s, %s, %s) returning id""",
                (num, title, ORG_ID),
            )
            chapter_id = int(cur.fetchone()[0])

            cur.execute(
                """INSERT into section(num, title, chapter_id) VALUES
                (%s, %s, %s) returning id""",
                ("0", "All rules", chapter_id),
            )
            section_id = int(cur.fetchone()[0])

            # print(chapter_match.group(1), chapter_match.group(2), chapter_id)
            rule_content = ""
            last_subrule_num = None
            is_rule_sublist = False
        else:
            rule_content += line
    print("Now manually copy over the last chapter")


import_lawbook()
conn.commit()
cur.close()
conn.close()
