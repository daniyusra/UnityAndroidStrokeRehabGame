using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.SceneManagement;

public class GameOverScreen : MonoBehaviour
{
    public Text pointsText;
    public static GameOverScreen inst;
    
    public void Setup (int score)
    {  
        Debug.Log("masuk setup");
        gameObject.SetActive(true);
        pointsText.text = score.ToString() + " POINTS";
    }

    public void RestartButton()
    {
        SceneManager.LoadScene("GameScene");
    }

    public void ExitButton()
    {
        SceneManager.LoadScene("MenuScene");
    }

}
