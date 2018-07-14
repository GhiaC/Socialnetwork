function setCookie(cname, cvalue, exdays) {
    var d = new Date();
    d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
    var expires = "expires=" + d.toUTCString();
    document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
}

function getCookie(cname) {
    var name = cname + "=";
    var ca = document.cookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return false;
}

function checkCookie() {
    var key = getCookie("key");
    if (key != "")
        //TODO check by server
        return true;
    return false;
}

function logOut() {
    setCookie('key','',-1000);
    setCookie('username','',-1000);
    window.location= "/";
}
function isLoggedIn() {
    return getCookie('username') && getCookie('key');
}