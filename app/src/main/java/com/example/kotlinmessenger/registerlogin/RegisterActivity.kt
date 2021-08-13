package com.example.kotlinmessenger.registerlogin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.kotlinmessenger.messages.LatestMessagesActivity
import com.example.kotlinmessenger.R
import com.example.kotlinmessenger.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //'회원가입 버튼' 눌렀을 때
        register_button_register.setOnClickListener {
            performRegister() //회원가입 함수 호출
        }

        //'이미 회원인경우 로그인 텍스트' 눌렀을 때
        already_have_account_text_VIew.setOnClickListener {
            Log.d("RegisterActivity","try to show login activity")

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        //프로필 사진 버튼 누를 때
        selectphoto_button_register.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent,0) // 사진첩 열림
            //startActivityForResult()메소드를 통해 서브액티비티를 만들고 액티비티끼리 서로 데이터를 교환 할 수 있음
            // 자식 액티비티의 결과를 부모 액티비티에서 처리할 때 사용한다.
            // 자식 액티비티의 결과를 부모 액티비티에서 처리 안 할 때는 startActivity()사용
        }
    }

    //-----------------------------------------함수------------------------------------------------------


    var selectedPhotoUri: Uri? =null
    // 사진첩 액티비티에서 register액티비티으로 결과값이 넘어 올 때 호출된다.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //리퀘스트코드가 0이거나, RESULT_OK는 액티비티 실행이 성공임을 뜻 함,
        //리퀘스트코드가 0이거나(사진첩 리퀘스트코드 0), 액티비티가 잘 실행되거나, 데이터가 null이 아닐 때 데이터를 가져옴
        if(requestCode ==0 && resultCode == Activity.RESULT_OK && data !=null ){
            Log.d("RegisterActivity","사진이 선택되었다.")

            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver,selectedPhotoUri)

            selectphoto_imageview_register.setImageBitmap(bitmap)
            selectphoto_button_register.alpha =0f
//            val bitmapDrawable = BitmapDrawable(bitmap)
//            selectphoto_button_register.setBackgroundDrawable(bitmapDrawable)
        }
    }
    //회원가입 함수
    private fun performRegister(){
        val email = email_edittext_register.text.toString()
        val password = password_edittext_register.text.toString()

        if(email.isEmpty()||password.isEmpty()){  // 아무것도 입력안하고 register버튼 눌렀을 때
            Toast.makeText(this,"이메일과 패스워드 입력해주세요",Toast.LENGTH_SHORT).show()
            return
        }

        //회원가입 회원 생성
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener{
                if (!it.isSuccessful) return@addOnCompleteListener

                // else if successful
                Log.d("RegisterActivity","Successfully created user with uid: ${it.result?.user?.uid}")

                uploadImageToFirebaseStorage()
            }
            .addOnFailureListener {//이메일 형식에 맞지 않을 때
                Toast.makeText(this,"Failed to create User",Toast.LENGTH_SHORT).show()
                Log.d("RegisterActivity", "Failed to create User: ${it.message}")
            }
    }
    //이미지를 파이어베이스 스토리지에 업로드하는 함수
    private fun uploadImageToFirebaseStorage(){
        if(selectedPhotoUri == null) return
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d("RegisterActivity","이미지 업로드 성공: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    it.toString()  // 주소
                    Log.d("RegisterActivity", "FileLocation: $it")
                    // https://firebasestorage.googleapis.com/v0/b/~이런 식의 파일 주소

                    saveUserToFirebaseDatabase(it.toString())
                }
            }
            .addOnFailureListener{

            }
    }

    //사용자를 리얼타임 디비에 넣는 함수
    private fun saveUserToFirebaseDatabase(profileImageUrl: String){
        val uid = FirebaseAuth.getInstance().uid?:""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        val user = User(uid, username_edittext_register.text.toString(),profileImageUrl)
        ref.setValue(user)
            .addOnSuccessListener {
                Log.d("RegisterActivity","리얼타임디비에 사용자 정보 넣기 성공" )

                val intent = Intent(this, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
    }
}
