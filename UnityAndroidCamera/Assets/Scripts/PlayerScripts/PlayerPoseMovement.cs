using UnityEngine;
using UnityEngine.SceneManagement;

public class PlayerPoseMovement : MonoBehaviour
{
    bool alive = true;

    public float speed = 5;
    [SerializeField] Rigidbody rb;

    float horizontalInput;
    [SerializeField] float horizontalMultiplier = 2;

    public float speedIncreasePerPoint = 0.1f;

    [SerializeField] float jumpForce = 400f;
    [SerializeField] LayerMask groundMask;
    private AndroidJavaObject _androidJavaPlugin = null;
    float maxDeg = 60F; //assumed max degree 
    int maxX = 6; //max horizontal displacement

    

    void Start()
    {
        if (Application.platform == RuntimePlatform.Android)
        {
            using (AndroidJavaClass javaClass = new AndroidJavaClass("arp.camera.CameraPluginActivity"))
            {
                _androidJavaPlugin = javaClass.GetStatic<AndroidJavaObject>("_context");
            }

            //CreateTextureAndPassToPlugin();
            //yield return StartCoroutine("CallPluginAtEndOfFrames");
        }
    }

    private void FixedUpdate()
    {
        if (!alive) return;

        Vector3 forwardMove = transform.forward * speed * Time.fixedDeltaTime;
        //Vector3 horizontalMove = transform.right * horizontalInput * speed * Time.fixedDeltaTime * horizontalMultiplier;
        rb.MovePosition(rb.position + forwardMove);
        Debug.Log(rb.position.x + " " + rb.position.y + " " + rb.position.z);
    }

    // Update is called once per frame
    void Update()
    {
        horizontalInput = Input.GetAxis("Horizontal");

        float playerAngle = _androidJavaPlugin.Call<float>("returnPersonAngle");

        if (Mathf.Abs(playerAngle) > maxDeg)
        {
            playerAngle = maxDeg * Mathf.Abs(playerAngle) / playerAngle;
        }

        playerAngle = Mathf.Round(playerAngle);
        playerAngle = -playerAngle;

        Vector3 horizontalMove = transform.right * (((playerAngle/maxDeg*maxX)- rb.position.x));

        rb.MovePosition(rb.position + horizontalMove);


        if (Input.GetKeyDown(KeyCode.Space))
        {
            Jump();
        }

        if (transform.position.y < -5)
        {
            Die();
        }
    }

    public void Die()
    {
        alive = false;
        // restart the game
        Invoke("Restart", 2);
    }

    void Restart()
    {
        SceneManager.LoadScene(SceneManager.GetActiveScene().name);
    }

    void Jump()
    {
        // check if grounded
        float height = GetComponent<Collider>().bounds.size.y;
        bool isGrounded = Physics.Raycast(transform.position, Vector3.down, (height / 2) + 0.1f, groundMask);

        // if we are, jump
        rb.AddForce(Vector3.up * jumpForce);
    }
}
