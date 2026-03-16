import java.io.File
import java.io.*


// checking if user is in the database and if so check if their password is correct

fun readWordList(username: String): MutableList<String> { 
    var filename= "/workspaces/1860worksheets/year2/final-project/welcome-page/userdatabase" // this would change 
    var password= MutableList<String>(5) 
    val Invalid= "username not found"

    File(filename).forEachLine{
        if (it == username){
            password.set(it)
        } 

    if (password.size == 0 ){
        return Invalid
    }
    }      
    return password 
}


// Registerig a user: adding them to the database 

class Person(val username: String, val password:String, val email: String, val firstName:String, val secondName: String ){
    

    fun storinguser(firstName: String, secondName:String, email: String,username:String, password:String) {
        val file = File("userdatabase.txt") // THIS WOULD ALSO NEED TO CHANGE 

        //val line = "$username, $password, $email, $firstName, $secondName \n" 
        //file.appendText(line)
        val person = Person(firstName,secondName,email,username,password)
        File("userdatabase.txt").appendText("${person.username}, ${person.password}, ${person.firstName}, ${person.secondName}, ${person.email}\n")

    }

    
    fun authenticate(username:String, password:String):Boolean{
        var stored_password= readWordList(username)

        if (stored_password == password){
            return True 
        } else {
            return False 
        }
    } // only for when users are already signed up


}

// for botton options: when sign in is choosen 
fun signin() {
    if (sign-up =True){
        // user input their information, call storing user to input 
    }
}

fun main(){
    var file= String
    file= "/workspaces/1860worksheets/year2/final-project/welcome-page/userdatabase" //ignore 


}