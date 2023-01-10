import json
import os

import requests
from django.contrib import auth
from django.contrib.auth.decorators import login_required

from django.http import HttpResponseRedirect
from django.shortcuts import render, redirect, HttpResponse
from django.views.decorators.csrf import csrf_exempt
from requests_toolbelt import MultipartEncoder

from . import forms
from django.contrib.auth.models import User

from .forms import UploadFileForm


def login(request):
    if request.session.get('is_login', None):  # 不允许重复登录
        return redirect('/index/')
    if reversed('login'):
        if request.method == "POST":
            login_form = forms.UserForm(request.POST)
            message = "请检查填写的内容！"
            if login_form:
                username = request.POST.get("username")
                password = request.POST.get("password")
                try:
                    user = User.objects.get(username=username)
                except:
                    message = "用户名或密码不正确！"
                    return render(request, 'system/login.html', locals())
                if user.check_password(password):
                    request.session['is_login'] = True
                    request.session['user_name'] = user.username
                    auth.login(request, user)
                    return redirect('/index/')
                else:
                    message = "用户名或密码不正确！"
                    return render(request, 'system/login.html', locals())
            else:
                return render(request, "system/login.html", locals())
        login_form = User()
        return render(request, 'system/login.html', locals())


@login_required()
def index(request):
    # print("*******",request.session.get('user_name'),"********")
    url = "http://127.0.0.1:25300/file/getAll/?bucketName=" + request.session.get('user_name')
    lr = requests.get(url)
    request.session["all_files"] = lr.json()
    if reversed('/index/'):
        return render(request, 'system/index.html')


def register(request):
    if reversed('register'):
        if request.session.get("is_login", None):
            return redirect('/index/')
        if request.method == "POST":
            register_form = forms.RegisterForm(request.POST)
            message = "请检查填写的内容！"
            if register_form.is_valid():  # 判断表单的数据是否合法，返回一个布尔值
                username = register_form.cleaned_data.get('username')
                password1 = register_form.cleaned_data.get('password1')
                password2 = register_form.cleaned_data.get('password2')
                email = register_form.cleaned_data.get('email')

                if password1 != password2:
                    message = '两次输入的密码不同！'
                    return render(request, 'system/register.html', locals())
                else:
                    same_name_user = User.objects.filter(username=username)
                    if same_name_user:
                        message = '用户名已经存在'
                        return render(request, 'system/register.html', locals())
                    same_email_user = User.objects.filter(email=email)
                    if same_email_user:
                        message = '该邮箱已经被注册了！'
                        return render(request, 'system/register.html', locals())
                    new_user = User()
                    new_user.username = username
                    new_user.email = email
                    new_user.set_password(password1)
                    new_user.save()

                    return redirect('/login/')
            else:
                return render(request, 'system/register.html', locals())
        register_form = forms.RegisterForm()
        return render(request, 'system/register.html', locals())


def logout(request):
    if reversed('logout'):
        auth.logout(request)
        return redirect("/login/")


@csrf_exempt
def upload_file(request):

    form = UploadFileForm(request.POST, request.FILES)
    url = 'http://127.0.0.1:25300/file/upload?bucketName=testuser1'
    r = requests.post(url, files=request.FILES)
    return render(request, 'system/uploadFile.html', {'form': form})


@csrf_exempt
def uploadFile(request):
    if reversed('uploadFile'):
        auth.logout(request)
        return render(request, 'system/uploadFile.html')

# def handle_uploaded_file(f):
#     url = '127.0.0.1:25300/file/uploadPath?bucketName=testuser1&path=jotmp/'
#     r = requests.post(server('new'), files={'content': open('test.md', 'rb')})
#     with open('some/file/name.txt', 'wb+') as destination:
#         for chunk in f.chunks():
#             destination.write(chunk)
