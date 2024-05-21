# Spring Boot 3 Security with JWT
## I- JWT
![jwt-process.png](src%2Fmain%2Fresources%2Fimages%2Fjwt-process.png)
* 1- request is send to our custom filter
* 2- if header request don't have jwt token we return 403 either we continue to jwt service who will extract from token the user name or email etc..
* 3- there is not user define in token we retrieve 403
* 4- user is extracted well, we ask the db if that user exist
* 5- user not exist we return 403 or if exist we back to jwtService to validate jwt token
* 6- token is not valid (expired etc...) return 403 
* 7- token is valid we set the context to notify that user is authenticated
* 8- we can go to sprint Authorisation Process now (we will define it after)
* 9- we return to the client 200 to let know that the user is well authenticated

#### How to make our application work
- Start by running postgres docker image with environment variables and port mapping
application.yml most have the same environment variable and port than docker db
```docker
docker run -d -e POSTGRES_PASSWORD=secret -e POSTGRES_DB=springbootsecuritydb -e POSTGRES_USER=pw --name postgresdb -p 5433:5432 postgres
```
- To connect from host according to environment variable 
```bash
psql -p $LOCAL_PORT -h 127.0.0.1 -d $YOUR_DB_NAME -U $YOUR_USERNAME -W
psql -p 5433 -h 127.0.0.1 -d springbootsecuritydb -U pw -W
```
- start spring boot application it will create needed table on db

## What we will build
The API must expose routes where some are accessible without authentication while others require one. Below are the routes:

* [POST] /auth/signup → Register a new user
* [POST] /auth/login → Authenticate a user
* [GET] /users/me → Retrieve the current authenticated user
* [GET] /users → Retrieve the current authenticated user
The routes “/auth/signup” and “/auth/login” can be accessed without authentication while “users/me” and “users” require to be authenticated.

### Install JSON Web Token dependencies

```xml
<dependencies>
    <!---- existing dependencies here....... ---->
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
  </dependency>
</dependencies>
```
#### 1- Create the user entity
We perform authentication to ensure the users accessing the system are known, meaning they are stored in the database.

#### 2- create UserRepository

### 3- Extend the User Entity with authentication details
To manage user details related to authentication, Spring Security provides an Interface named “UserDetails” with properties and methods that the User entity must override the implementation.
Update the file “User.java to” implement the UserDetails interface

### 4-Create the JWT service
To generate, decode, or validate a JSON Web token, we must expose the related methods that use the libraries we installed earlier. We will group them into a service class named JwtService.

### 5- Override the security configuration in ApplicationConfiguration.java file

By default, the HTTP basic authentication, but we want to override it to perform the:

* Perform the authentication by finding the user in our database.
* Generate a JWT token when the authentication succeeds.

### 6- Create the authentication middleware in JwtAuthenticationFilter.java file
For every request, we want to retrieve the JWT token in the header “Authorization”, and validate it:

* If the token is invalid, reject the request if the token is invalid or continues otherwise.
* If the token is valid, extract the username, find the related user in the database, and set it in the authentication context so you can access it in any application layer.

### 7- Configure the application requester filter in SecurityConfiguration.java

The custom authentication is ready, and the remaining thing is to define what criteria an incoming request must match before being forwarded to application middleware. We want the following criteria:

* There is no need to provide the CSRF token because we will use it.
* The request URL path matching /auth/signup and /auth/login doesn't require authentication.
Any other request URL path must be authenticated.
* The request is stateless, meaning every request must be treated as a new one, even if it comes from the same client or has been received earlier.
* Must use the custom authentication provider, and they must be executed before the authentication middleware.
* The CORS configuration must allow only POST and GET requests.

### 8- Create the authentication service (AuthenticationService.java) and RegisterUserDto.java, LoginUserDto.java

### 9- Create user registration and authentication routes (AuthenticationController.java)
We can now create the routes /auth/signup and /auth/login for user registration and authentication, respectively.

### Test the implementation with postman (start docker postgres image wit db after that start spring boot)
Run the application, open an HTTP client, and send a POST request to /auth/signup with the information in the request body.

![signup.png](src%2Fmain%2Fresources%2Fimages%2Fsignup.png)

* Now, let’s try to authenticate with the user we registered. Send a POST request to /auth/login with the information in the request body.
 and generate token for registered user

![generate-token.png](src%2Fmain%2Fresources%2Fimages%2Fgenerate-token.png)

### 10- Create restricted endpoints to retrieve users (file UserController.java, )
The endpoints /users/me and /users respectively return the authenticated user from the JWT token provided and the list of all the users.
* in controllers  we retrieve the authenticated user from the security context that has been set in the file JwtAuthenticationFilter.java at line 68

### 10.1 - Test the implementation
Re-run the application and follow this scenario:

* Send a GET request to /users/me and /users, you will get a 403 error (because in SecurityConfiguration all request that those not start by /auth/** must be authenticated)
* Authenticate with POST request at /auth/login and obtain the JWT token.
* Put the JWT token in the authorization header of the request /users/me and /users; you will get an HTTP response code 200 with the data.

![add-token-to-request.png](src%2Fmain%2Fresources%2Fimages%2Fadd-token-to-request.png)

### 11- Customize authentication error messages
There are different authentications we want to return a more explicit message. Let’s enumerates them:

* Bad login credentials: thrown by the exception BadCredentialsException, we must return the HTTP Status code 401.
* Account locked: thrown by the exception AccountStatusException, we must return the HTTP Status code 403.
* Not authorized to access a resource: thrown by the exception AccessDeniedException, we must return the HTTP Status code 403.
* Invalid JWT: thrown by the exception SignatureException, we must return the HTTP Status code 401.
* JWT has expired: thrown by the exception ExpiredJwtException, we must return the HTTP Status code 401.

To handle these errors, we must use the Spring global exception handler to catch the exception thrown and customize the response to send to the client.
Create a package exceptions then create a file named GlobalExceptionHandler.java

- Re-run the application and try to authenticate with invalid credentials, send a request with an expired JWT or an invalid JWT, etc…

![test-errors.png](src%2Fmain%2Fresources%2Fimages%2Ftest-errors.png)

The JWT exceptions are caught by the global exception handler because we used the Handler exception resolver in the file JwtAuthenticationFilter.java to forward them. The other exceptions come from Spring security.

### 12- Wrap up
In this post, we saw how to implement the JSON Web Token authentication in a Spring Boot application. Here are the main steps of this process:

* A JWT authentication filter extracts and validates the token from the request header.
* Whitelist some API routes and protect those requiring a token.
* Perform the authentication, generate the JWT, and set an expiration time.
* Use the JWT generated to access protected routes.
* Catch authentication exceptions to customize the response sent to the client.

With this implementation, you have the basis to protect your API, and you can go a step further by implementing a Role-Based Access Control (https://medium.com/@tericcabrel/implement-role-based-access-control-in-spring-boot-3-a31c87c2be5c) following my tutorial to restrict a resource based on the user role and permission.