"use strict";
{  // To avoid variables ending up in the global scope
    const loginForm = document.getElementById("login-form");
    const loginErrorMessage = document.getElementById("login-errormsg");

    const LoginHandler = function(_errorMessage) {
        this.errorMessage = _errorMessage;
        this.username = "";  // Set by handleEvent
        const self = this;

        this.callback = function (req) {
            const resp = req.responseText;
            switch (req.status) {
                case 200:
                    sessionStorage.setItem("username", self.username);
                    sessionStorage.setItem("loginTimestamp", JSON.stringify(new Date()));
                    window.location.href = "home.html";
                    break;
                case 400:
                    self.errorMessage.textContent =
                        "Error: " + getErrorMessage(resp);
                    break;
                case 401:
                    self.errorMessage.textContent =
                        "Error: wrong username or password";
                    break;
                default:
                    self.errorMessage.textContent =
                        "Login error";
            }
        };

        this.handleEvent = function (e) {
            e.preventDefault();
            self.errorMessage.textContent = "";

            if (e.target.checkValidity()) {  // Form contains valid data
                this.username = e.target.user.value;
                const paramList = {
                    user: e.target.user.value,
                    pass: e.target.pass.value
                };
                e.target.reset();
                makeCall("POST", "doLogin", paramList, self.callback);
            } else {
                e.target.reportValidity();
            }
        };
    };

    loginForm.addEventListener("submit", (new LoginHandler(loginErrorMessage)), false);
}