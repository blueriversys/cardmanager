@echo off
echo --- attempting to list files, but it must fail
java -jar target\manager-0.0.1-SNAPSHOT.jar -list

echo --- initializing card
java -jar target\manager-0.0.1-SNAPSHOT.jar -init,greatpass

echo --- this must print 'card already initialized'
java -jar target\manager-0.0.1-SNAPSHOT.jar -init,greatpass

echo --- this must print "invalid password"
java -jar target\manager-0.0.1-SNAPSHOT.jar -delall,great

echo --- delete all files in the card, must succeed
java -jar target\manager-0.0.1-SNAPSHOT.jar -delall,greatpass

echo --- add 3 files to the card
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/logo.jpg,picture1
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/tesla.jpg,picture2

echo --- this should list 2 files
java -jar target\manager-0.0.1-SNAPSHOT.jar -list

echo --- must print "picture1 already in the card"
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/logo.jpg,picture1

echo --- continue adding 4 more files
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/AlicePrivKey.txt,keyfile3
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/AlicePubKey.txt,keyfile4
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/BobPrivKey.txt,keyfile5
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/BobPrivKeyCert.txt,keyfile6
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/BobPubKey.txt,keyfile7

echo --- trying adding 1 more. Must fail.
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/BobRevocCert.txt,keyfile8

echo --- retrieve 2 files
java -jar target\manager-0.0.1-SNAPSHOT.jar -retrieve,retrievedfiles/retrievedlogo.jpg,picture1,greatpass
java -jar target\manager-0.0.1-SNAPSHOT.jar -retrieve,retrievedfiles/retrivedtesla.jpg,picture2,greatpass

echo --- try to retrieve a file that doesn't exist. Must fail
java -jar target\manager-0.0.1-SNAPSHOT.jar -retrieve,retrievedlogo.jpg,picture8,greatpass

echo --- delete all files
java -jar target\manager-0.0.1-SNAPSHOT.jar -delall,greatpass

echo --- list files. Must print "no files found on the card."
java -jar target\manager-0.0.1-SNAPSHOT.jar -list

echo --- this must fail.
java -jar target\manager-0.0.1-SNAPSHOT.jar -retrieve,retrievedfiles/keyfile6.txt,picture6,greatpass

echo --- will print wrong password (greatpas)
java -jar target\manager-0.0.1-SNAPSHOT.jar -changepass,newpass,greatpas

echo --- must change password to newpass
java -jar target\manager-0.0.1-SNAPSHOT.jar -changepass,newpass,greatpass

echo --- adding 2 files
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/AlicePrivKey.txt,keyfile1
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/AlicePubKey.txt,keyfile2
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/BobPrivKey.txt,keyfile3
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/BobPrivKeyCert.txt,keyfile4
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/BobPubKey.txt,keyfile5
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/BobPubKeyCert.txt,keyfile6
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,origfiles/BobRevocCert.txt,keyfile7


echo --- this should list 7 files
echo keyfile1: 883
echo keyfile1: 767
echo keyfile1: 5265
echo keyfile1: 5197
echo keyfile1: 2583
echo keyfile1: 2515
echo keyfile1: 728
java -jar target\manager-0.0.1-SNAPSHOT.jar -list

echo --- retrieve 1 file with password newpass
java -jar target\manager-0.0.1-SNAPSHOT.jar -retrieve,retrievedtesla.jpg,picture1,newpass

echo --- store a private key file into card
java -jar target\manager-0.0.1-SNAPSHOT.jar -add,privatekey.txt,privatekey

echo --- retrieve a private key file from card
java -jar target\manager-0.0.1-SNAPSHOT.jar -retrieve,retrievedfiles/keyfile3.txt,keyfile3,newpass

echo --- compare both files. There must be no difference between them
wc origfiles/BobPrivKey.txt retrievedfiles/keyfile3.txt

echo --- change password back to greatpass
java -jar target\manager-0.0.1-SNAPSHOT.jar -changepass,greatpass,newpass

