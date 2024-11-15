#!/bin/bash -eu

echo "Starting intTests script"
USE_XVFB=0
if [[ -z ${DISPLAY:-} ]]; then
    USE_XVFB=1
    TMPDIR=`mktemp -d`
    echo "starting Xvfb"
    Xvfb -screen 0, 1024x768x24 -displayfd 6 6>$TMPDIR/display.log &
    while true; do
        CONT=`cat $TMPDIR/display.log`
        if [[ -n $CONT ]]; then
            break
        fi
        sleep 1
    done
    export DISPLAY=:$CONT
    echo "DISPLAY=$DISPLAY"
    XVFB_PID=$!
fi

FAIL=0

if ! ./gradlew integrationTest -x test ; then
    FAIL=1
    cat build/test-results/integrationTest/*.xml || true
fi

if [[ $USE_XVFB = 1 ]]; then
    echo "stopping Xvfb"
    kill $XVFB_PID
    rm -rf $TMPDIR
fi

echo "Finishing intTests script with exit code $FAIL"
exit $FAIL
