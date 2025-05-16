from django.forms.renderers import TemplatesSetting


class BootstrapFormRenderer(TemplatesSetting):
    form_template_name = "bootstrap_field.html"
