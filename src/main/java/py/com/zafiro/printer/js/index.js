/**
 * Created by eratzlaff on 14/12/15.
 */

angular.module('app', ['ngWebSocket']).controller('WSApp', function ($scope, $location, $websocket) {
    if ($location.protocol() === 'http') {
        if ($location.port() === 7612 || $location.port() === 80) {
            $scope.wsServer = "ws://" + $location.host() + ":" + $location.port() + "/api/ws";
        } else {
            $scope.wsServer = "ws://" + $location.host() + ":" + 7612 + "/api/ws";
        }
    } else {
        if ($location.port() === 7613 || $location.port() === 443) {
            $scope.wsServer = "wss://" + $location.host() + ":" + $location.port() + "/api/ws";
        } else {
            $scope.wsServer = "wss://" + $location.host() + ":" + 7613 + "/api/ws";
        }
    }

    $scope.logs = [];
    $scope.online = false;
    var options = {
        reconnectIfNotNormalClose: true
    };

    var dataStream = $websocket($scope.wsServer, options);

    dataStream.onMessage(function (message) {
        var data = JSON.parse(message.data);
        if (data.cmd === 'register') {
            $scope.register = data;
            $scope.printers = data.status;
        } else if (data.cmd === 'status') {
            $scope.register.online = data.online;
            $scope.printers = data.status;
        } else {
            $scope.logs.push(JSON.parse(message.data));
        }
    });

    dataStream.onClose(function (data) {
        $scope.online = false;
    });

    dataStream.onOpen(function (data) {
        $scope.online = true;
    });

});