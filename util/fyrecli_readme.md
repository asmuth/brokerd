


# publishing events 

    $ fyrecli push "{ 'type': 'unicorn', 'message': 'test' }"

    $ fyrecli push --type "unicorn" --message "test"

    $ fyrecli -h 1.1.1.1:2323 push --type "broadcast" --message "test"

    $ echo "{ 'type': 'broadcast', 'message': 'test' }" | fyrecli -h 1.1.1.1:2323 push



# subscribing to events

    $ fyrecli "stream where(channel = 'dawanda.status')" | pv > /dev/null

    $ fyrecli "stream since(-60sec)" | pv > /dev/null


# loop!

    $ fyrecli stream | fyrecli push

