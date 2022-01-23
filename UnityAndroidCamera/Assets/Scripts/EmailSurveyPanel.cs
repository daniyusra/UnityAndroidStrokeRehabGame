using System.Collections;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using UnityEngine;
using UnityEngine.UI;
using System.Data.SqlClient;
using UnityEngine.Networking;
using System;
using System.Text;

public class EmailSurveyPanel : MonoBehaviour
{
    // Start is called before the first frame update
    public InputField Field;
    public const string MatchEmailPattern =
           @"^(([\w-]+\.)+[\w-]+|([a-zA-Z]{1}|[\w-]{2,}))@"
           + @"((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\."
             + @"([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\.([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
           + @"([a-zA-Z]+[\w-]+\.)+[a-zA-Z]{2,4})$";


    void Start()
    {
        
    }

    // Update is called once per frame
    void Update()
    {
        
    }

    public void GetAndSendEmail()
    {
        string email = Field.text;
        
        if (Regex.IsMatch(email, MatchEmailPattern))
        {
            
            
            MyClass myrequest = new MyClass();
            myrequest.taskName = email;
            myrequest.isComplete = true;
            //StartCoroutine(SendMail(email));



            string json = JsonUtility.ToJson(myrequest);
            StartCoroutine(SendMail(json));

            //gameObject.SetActive(false);
        } else
        {
            Field.text = "Put proper email!";
        }

    }

    [Serializable]
    public class MyClass
    {
        public string taskName;
        public bool isComplete;
    }

    IEnumerator SendMail(string jsonstring)
    {
        var request = new UnityWebRequest("https://virtuostroke.azurewebsites.net/api/todoitems", "POST");
        byte[] bodyRaw = Encoding.UTF8.GetBytes(jsonstring);
        request.uploadHandler = (UploadHandler)new UploadHandlerRaw(bodyRaw);
        request.downloadHandler = (DownloadHandler)new DownloadHandlerBuffer();
        request.SetRequestHeader("Content-Type", "application/json");
        yield return request.SendWebRequest();
        Debug.Log("Status Code: " + request.responseCode);
        gameObject.SetActive(false);

    }

    public void CloseForm()
    {
        gameObject.SetActive(false);
    }
}
