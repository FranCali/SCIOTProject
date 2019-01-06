# SCIOTProject

This project has been developed as final project for our university exam of Serverless Computing for IoT. 
The project was born from the idea of giving to computer science students of our university the possibility to monitor study rooms informations in real time by using an android app with a simple interface. 
The project is based on two main modules: seats book/release and chat. Users can see current seats in three study rooms, can talk on a dedicated chat. 

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
- OS: 
    - Ubuntu 18.04 LTS
- Software:
    - Docker and Docker Compose (Application containers engine)
    - Nuclio (Serverless computing provider)
    - RabbitMQ (AMQP and MQTT message broker)

```
Android Studio: https://developer.android.com/studio/
Docker CE: https://docs.docker.com/install/linux/docker-ce/ubuntu/#extra-steps-for-aufs
```

### Installing

Run Nuclio container
```sh
$ docker run -p 8070:8070 -v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp nuclio/dashboard:stable-amd64
```
Run RabbitMQ container
```sh
$ sudo docker run -p 9000:15672  -p 1883:1883 -p 5672:5672  cyrilix/rabbitmq-mqtt 
```

Install android app on smartphone via Android Studio

## Authors

-**Francesco Califano** 
-**Pietro Russo**

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.
