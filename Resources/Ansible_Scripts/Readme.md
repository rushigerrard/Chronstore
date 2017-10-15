Steps to run the Ansible Script:
 1.  You will need a github token which has access to private repositories.
 2.  Install Ansible on your machine.
 2.  ansible-playbook main.yml --private-key=**PRIVATE_KEY_PATH** -u**REMOTE_USER_NAME** --extra-vars="UNITY_ID=**YOUR_UNITY_ID** GIT_TOKEN=**YOUR_GIT_TOKEN** IP_ADDR=**VCL_IP_ADDRESS** GIT_BRANCH=**GIT_BRANCH_NAME**"
 3.  This will prompt you to enter the password. Enter Password associated with your Unity ID

####To run docker image on remote
`docker run -v ~/DockerImage/keyvalue/:/root/KeyValueStore/ keyvalue &`
d5b4a6611363172dce6dee49cf9db293cfac6fde
ansible-playbook main.yml --private-key=~/.ssh/id_rsa -uaabarve --extra-vars="UNITY_ID=aabarve GIT_TOKEN=d5b4a6611363172dce6dee49cf9db293cfac6fde IP_ADDR=152.7.99.153 GIT_BRANCH=master