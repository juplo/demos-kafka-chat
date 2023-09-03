#!/bin/bash
mongoimport --collection=chatRoomTo --db=test /docker-entrypoint-initdb.d/chatRoomTo.json
mongoimport --collection=messageTo --db=test /docker-entrypoint-initdb.d/messageTo.json
