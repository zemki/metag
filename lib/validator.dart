import 'dart:async';

mixin Validator{
    
    var emailValidator = StreamTransformer<String,String>.fromHandlers(
      handleData: (email, sink){
        
        if(email.length > 3 && email.contains('@')){
          sink.add(email);
        }else{
          sink.addError("Email is not valid");
        }
      }
    );
    
    var passwordValidator = StreamTransformer<String,String>.fromHandlers(
      handleData: (password, sink){
        if(password.length > 1){
          sink.add(password);
        }else{
          sink.addError("Password is too short");
        }
      }
    );


}