# Generated by Django 5.1.6 on 2025-03-02 15:24

import django.db.models.deletion
from django.db import migrations, models


class Migration(migrations.Migration):

    initial = True

    dependencies = [
        ('dst', '__first__'),
    ]

    operations = [
        migrations.CreateModel(
            name='StudentRequiredMinutes',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('fromdate', models.DateField()),
                ('required_minutes', models.IntegerField()),
                ('person', models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name='required_minutes', to='dst.person')),
            ],
        ),
        migrations.CreateModel(
            name='Year',
            fields=[
                ('id', models.AutoField(primary_key=True, serialize=False)),
                ('from_time', models.DateTimeField()),
                ('to_time', models.DateTimeField()),
                ('inserted_date', models.DateTimeField(auto_now_add=True)),
                ('name', models.TextField()),
                ('organization', models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, to='dst.organization')),
            ],
        ),
        migrations.CreateModel(
            name='Excuse',
            fields=[
                ('id', models.AutoField(primary_key=True, serialize=False)),
                ('inserted_date', models.DateTimeField(auto_now_add=True)),
                ('date', models.DateField()),
                ('person', models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, to='dst.person')),
            ],
            options={
                'constraints': [models.UniqueConstraint(models.F('person'), models.F('date'), name='uniq_excuses_person_date')],
            },
        ),
        migrations.CreateModel(
            name='Override',
            fields=[
                ('id', models.AutoField(primary_key=True, serialize=False)),
                ('inserted_date', models.DateTimeField(auto_now_add=True)),
                ('date', models.DateField()),
                ('person', models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, to='dst.person')),
            ],
            options={
                'constraints': [models.UniqueConstraint(models.F('person'), models.F('date'), name='uniq_overrides_person_date')],
            },
        ),
        migrations.CreateModel(
            name='Swipe',
            fields=[
                ('id', models.AutoField(primary_key=True, serialize=False)),
                ('swipe_day', models.DateField()),
                ('in_time', models.DateTimeField()),
                ('out_time', models.DateTimeField(blank=True, null=True)),
                ('person', models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, to='dst.person')),
            ],
            options={
                'indexes': [models.Index(models.F('person'), models.F('swipe_day'), name='swipes_person_id_swipe_day_idx'), models.Index(models.F('swipe_day'), models.F('person'), name='swipes_swipe_day_person_id_idx')],
            },
        ),
    ]
