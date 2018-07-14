function isLogin() {
    localforage.setItem('online', false).then(function (value) {
        console.log(value);
    }).catch(function (err) {
        console.log(err);
    });
    var key = null;
    localforage.getItem('user', function (err, value) {
        key = value.key;
    });
    axios.post('http://localhost:8080/myframework/w/', {
        key: key
    })
        .then(function (response) {
            localforage.setItem('online', response.data.result).then(function (value) {
                alert(value);
                console.log(value);
            }).catch(function (err) {
                console.log(err);
            });
        })
        .catch(function (error) {
            alert(error);
        });

}