#pragma once

#include <jni.h>

struct mpv_node;

jobject mpv_node_to_jobject(JNIEnv *env, const mpv_node *node);
int jobject_to_mpv_node(JNIEnv *env, jobject jnode, mpv_node *node);
void free_mpv_node(mpv_node *node);