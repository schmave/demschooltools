from django.contrib.auth.views import redirect_to_login
from django.views import View


class IndexView(View):
    def get(self, request):
        return redirect_to_login("")
