from django.db import migrations

# def create_new_users(apps, schema_editor):
#     """
#     Copy over Custodia users and passwords so that they can be used with new
#     Custodia and also the check-in app.
#     """
#     for custodia_user in (
#         # These are the schools that are using Custodia since 2025-01-01
#         OldUsers.objects.filter(school_id__in=(1, 2, 8, 11, 16))
#         .exclude(username="user")  # this is a random user that was in the DB
#         .exclude(roles__contains="admin")
#     ):
#         dst_user = User.objects.filter(
#             organization_id=custodia_user.school_id, name=CHECKIN_USERNAME
#         ).first()

#         if not dst_user:
#             dst_user = User.objects.create_user(
#                 custodia_user.username,
#                 custodia_user.username,
#                 custodia_user.password,
#                 name=CHECKIN_USERNAME,
#                 organization_id=custodia_user.school_id,
#                 email_validated=True,
#             )

#         dst_user.password = custodia_user.password
#         dst_user.save()


# def uncreate(apps, schema_editor):
#     User.objects.filter(date_joined__gt=datetime.now() - timedelta(days=1)).delete()


class Migration(migrations.Migration):
    dependencies = [
        ("custodia", "0001_initial"),
    ]

    operations = []  # migrations.RunPython(create_new_users, uncreate)]
