db.loggedEvents.remove({eventType:"USER_REGISTRATION"});
var users = db.users.find({});
users.forEach(function(user){
    db.loggedEvents.insert({
            eventType: "USER_REGISTRATION",
            userId: user._id.valueOf(),
            anonymousUser: false,
            timestamp: user.registrationDate
        });
});




