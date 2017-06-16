#include "log.h"

#include <stdlib.h>

void die(const char *msg)
{
    ALOGE("%s", msg);
    exit(1);
}
