from django.core.management import BaseCommand

from dst.models import Chapter, Entry, Organization, Section


class Command(BaseCommand):
    help = "Copies a management manual from one school to another"

    def handle(self, *args, year=0, **kwargs):
        tosv = Organization.objects.get(short_name="TOSV")
        old_tos = Organization.objects.get(short_name="TOS")

        old_chapter_to_new = {}

        for chapter in Chapter.objects.filter(organization=old_tos, deleted=False):
            old_id = chapter.id
            chapter.id = None
            chapter.organization = tosv
            chapter.save()
            old_chapter_to_new[old_id] = chapter.id

        print(old_chapter_to_new)

        old_section_to_new = {}

        for section in Section.objects.filter(
            chapter__organization=old_tos, deleted=False
        ):
            if section.chapter_id not in old_chapter_to_new:
                continue
            old_id = section.id
            section.id = None
            section.chapter_id = old_chapter_to_new[section.chapter_id]
            section.save()
            old_section_to_new[old_id] = section.id

        print(old_section_to_new)

        for entry in Entry.objects.filter(
            section__in=old_section_to_new.keys(), deleted=False
        ):
            entry.id = None
            entry.section_id = old_section_to_new[entry.section_id]
            entry.save()
