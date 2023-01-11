#!/bin/bash
mongoimport --collection=chatRoomTo --db=test /docker-entrypoint-initdb.d/chatRoomTo.json
