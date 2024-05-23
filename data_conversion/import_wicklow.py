import re

import psycopg2

ORG_ID = 10

conn = psycopg2.connect("dbname=school_crm user=postgres host=localhost port=5432")
cur = conn.cursor()
# cur.execute("set client_encoding to 'latin1'")


def import_names():
    cur.execute(
        "DELETE FROM person_tag WHERE person_id in (SELECT person_id from person where organization_id=%d)"
        % ORG_ID
    )
    cur.execute(
        "DELETE FROM person_tag_change WHERE person_id in (SELECT person_id from person where organization_id=%d)"
        % ORG_ID
    )
    cur.execute("DELETE FROM person WHERE organization_id=%d" % ORG_ID)

    cur.execute(
        "SELECT id from tag where title='Current Student' and organization_id=%d"
        % ORG_ID
    )
    student_tag_id = int(cur.fetchone()[0])

    cur.execute(
        "SELECT id from tag where title='Staff' and organization_id=%d" % ORG_ID
    )
    staff_tag_id = int(cur.fetchone()[0])

    is_student = False

    for name in open("names.txt"):
        if not name.strip():
            is_student = True
            continue

        splits = name.split(" ")
        first_name = splits[0].title()
        last_name = splits[1].title()

        cur.execute(
            """INSERT into person(
            first_name, last_name, organization_id) VALUES
            (%s, %s, %s) returning person_id""",
            (first_name, last_name, ORG_ID),
        )

        person_id = int(cur.fetchone()[0])

        cur.execute(
            "INSERT into person_tag (tag_id, person_id) VALUES (%s, %s)",
            (student_tag_id if is_student else staff_tag_id, person_id),
        )

        cur.execute(
            "INSERT into person_tag_change (tag_id, person_id, creator_id, was_add) VALUES (%s, %s, 1, true)",
            (student_tag_id if is_student else staff_tag_id, person_id),
        )


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

    for line in open("lawbook.txt"):
        chapter_match = re.match(r"Section (.*) - (.*)", line)
        section_match = re.match(r"(\d+) *- *(.*)", line)
        rule_match = re.match(r"(\d+\.\d+.*)\t(.*)", line)

        if chapter_match or section_match or rule_match:
            if rule_num and rule_title and rule_content:
                cur.execute(
                    """INSERT into entry(num, title, section_id, content)
                    VALUES (%s, %s, %s, %s)""",
                    (rule_num, rule_title, section_id, rule_content),
                )
                # print('RULE', rule_num, rule_title, rule_content)
                rule_content = ""

        if section_match:
            cur.execute(
                """INSERT into section(num, title, chapter_id) VALUES
                (%s, %s, %s) returning id""",
                (section_match.group(1), section_match.group(2).strip(), chapter_id),
            )
            section_id = int(cur.fetchone()[0])
            # print('SECTION', section_match.group(1), section_match.group(2))
            rule_content = ""
        elif rule_match:
            rule_content = ""
            splits = rule_match.group(1).split(".")
            rule_num = splits[1].strip()
            rule_title = rule_match.group(2).strip()
        elif chapter_match:
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
            # print(chapter_match.group(1), chapter_match.group(2), chapter_id)
            rule_content = ""
        else:
            rule_content += line
    print("Now manually copy over the last chapter")


import_names()
import_lawbook()
conn.commit()
cur.close()
conn.close()
