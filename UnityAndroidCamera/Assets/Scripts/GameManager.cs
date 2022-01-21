using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class GameManager : MonoBehaviour
{
    int score;
    public static GameManager inst;
    public GameOverScreen gameOverScreen;
    public EmailSurveyPanel emailSurveyPanel;


    [SerializeField] Text scoreText;
    [SerializeField] PlayerMovement playerMovement;
    

    public void GameOver()
    {
        Debug.Log("BRUH");
        int number = Random.Range(1, 10);

        //if(number == 2)
        if(true)
        {
            emailSurveyPanel.gameObject.SetActive(true);
        }

        gameOverScreen.Setup(score);
    }

    public void IncrementScore()
    {
        score++;
        scoreText.text = "SCORE: " + score;

        // increase player's speed
        playerMovement.speed += playerMovement.speedIncreasePerPoint;
    }

    private void Awake()
    {
        inst = this;
    }

    // Start is called before the first frame update
    private void Start()
    {

    }

    // Update is called once per frame
    private void Update()
    {

    }
}