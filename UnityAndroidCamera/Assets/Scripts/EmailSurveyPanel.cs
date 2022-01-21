using System.Collections;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using UnityEngine;
using UnityEngine.UI;
using System.Data.SqlClient;

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
        try { 
        if (Regex.IsMatch(email, MatchEmailPattern))
        {
            string connstring = "Server=tcp:unityposedetection.database.windows.net,1433;Initial Catalog=unityposedetection;Persist Security Info=False;User ID=daniyusra;Password=ImagineCup2022;MultipleActiveResultSets=False;Encrypt=True;TrustServerCertificate=False;Connection Timeout=30;";
            SqlConnection conn = new SqlConnection(connstring);
            conn.Open();
            //SqlCommand cmd = new SqlCommand("Insert Into userData Values ('COK5', 'COK', 'COK', '20120618 10:34:09 AM')", conn);
            string s = string.Format("Insert Into userData Values ('{0}','{1}','{2}','{3}'",email,"TEST", "TEST", System.DateTime.Now );
            SqlCommand cmd = new SqlCommand(s,conn);
            cmd.ExecuteNonQuery();
            conn.Close();
            gameObject.SetActive(false);
        } else
        {
            Field.text = "Put proper email!";
        }
        } catch (System.Exception e)
        {
            Debug.Log(e.Message);
        }

    }

    public void CloseForm()
    {
        gameObject.SetActive(false);
    }
}
