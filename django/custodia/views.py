from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.views import redirect_to_login
from django.http import HttpResponse
from django.shortcuts import redirect, render
from django.views import View


class IndexView(View):
    def get(self, request):
        if not request.user.is_authenticated:
            return redirect_to_login("")

        return HttpResponse(open("static/index.html").read())


class LoginView(View):
    def get(self, request):
        return render(request, "login.html")

    def post(self, request):
        username = request.POST["username"]
        password = request.POST["password"]
        user = authenticate(request, username=username, password=password)
        print("user", user, username, password)
        if user is not None:
            login(request, user)
            return redirect("/")

        return redirect_to_login("")


class LogoutView(View):
    def get(self, request):
        if request.user.is_authenticated:
            logout(request)
        return redirect("/")
