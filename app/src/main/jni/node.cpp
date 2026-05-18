#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <mpv/client.h>
#include "jni_utils.h"

void free_mpv_node(mpv_node *node);

jobject mpv_node_to_jobject(JNIEnv *env, const mpv_node *node) {
    if (!node) return NULL;

    switch (node->format) {
        case MPV_FORMAT_NONE: {
            return env->GetStaticObjectField(mpv_MPVNode_None, mpv_MPVNode_None_INSTANCE);
        }
        case MPV_FORMAT_STRING: {
            jstring jstr = env->NewStringUTF(node->u.string);
            jobject stringNode = env->NewObject(mpv_MPVNode_StringNode, mpv_MPVNode_StringNode_init, jstr);
            env->DeleteLocalRef(jstr);
            return stringNode;
        }
        case MPV_FORMAT_FLAG: {
            return env->NewObject(mpv_MPVNode_BooleanNode, mpv_MPVNode_BooleanNode_init, (jboolean)node->u.flag);
        }
        case MPV_FORMAT_INT64: {
            return env->NewObject(mpv_MPVNode_IntNode, mpv_MPVNode_IntNode_init, (jlong)node->u.int64);
        }
        case MPV_FORMAT_DOUBLE: {
            return env->NewObject(mpv_MPVNode_DoubleNode, mpv_MPVNode_DoubleNode_init, (jdouble)node->u.double_);
        }
        case MPV_FORMAT_NODE_ARRAY: {
            jobjectArray nodeArray = env->NewObjectArray(node->u.list->num, mpv_MPVNode, NULL);
            for (int i = 0; i < node->u.list->num; i++) {
                jobject childNode = mpv_node_to_jobject(env, &node->u.list->values[i]);
                if (childNode) {
                    env->SetObjectArrayElement(nodeArray, i, childNode);
                    env->DeleteLocalRef(childNode);
                }
            }
            jobject arrayNode = env->NewObject(mpv_MPVNode_ArrayNode, mpv_MPVNode_ArrayNode_init, nodeArray);
            env->DeleteLocalRef(nodeArray);
            return arrayNode;
        }
        case MPV_FORMAT_NODE_MAP: {
            jobject hashMap = env->NewObject(java_util_HashMap, java_util_HashMap_init);
            for (int i = 0; i < node->u.list->num; i++) {
                jstring key = env->NewStringUTF(node->u.list->keys[i]);
                jobject childNode = mpv_node_to_jobject(env, &node->u.list->values[i]);
                if (childNode) {
                    env->CallObjectMethod(hashMap, java_util_HashMap_put, key, childNode);
                    env->DeleteLocalRef(childNode);
                }
                env->DeleteLocalRef(key);
            }
            jobject mapNode = env->NewObject(mpv_MPVNode_MapNode, mpv_MPVNode_MapNode_init, hashMap);
            env->DeleteLocalRef(hashMap);
            return mapNode;
        }
        default:
            return NULL;
    }
}

// recursively adding all nodes for map and arrays
int jobject_to_mpv_node(JNIEnv *env, jobject jnode, mpv_node *node) {
    if (!jnode || !node) return -1;

    jclass nodeClass = env->GetObjectClass(jnode);

    if (env->IsInstanceOf(jnode, mpv_MPVNode_None)) {
        node->format = MPV_FORMAT_NONE;
        env->DeleteLocalRef(nodeClass);
        return 0;
    }

    if (env->IsInstanceOf(jnode, mpv_MPVNode_StringNode)) {
        jfieldID valueField = env->GetFieldID(mpv_MPVNode_StringNode, "value", "Ljava/lang/String;");
        jstring jstr = (jstring)env->GetObjectField(jnode, valueField);
        if (jstr) {
            const char *str = env->GetStringUTFChars(jstr, NULL);
            node->format = MPV_FORMAT_STRING;
            node->u.string = strdup(str);
            env->ReleaseStringUTFChars(jstr, str);
        } else {
            node->format = MPV_FORMAT_STRING;
            node->u.string = strdup("");
        }
        env->DeleteLocalRef(nodeClass);
        return 0;
    }

    if (env->IsInstanceOf(jnode, mpv_MPVNode_BooleanNode)) {
        jfieldID valueField = env->GetFieldID(mpv_MPVNode_BooleanNode, "value", "Z");
        jboolean flag = env->GetBooleanField(jnode, valueField);
        node->format = MPV_FORMAT_FLAG;
        node->u.flag = flag;
        env->DeleteLocalRef(nodeClass);
        return 0;
    }

    if (env->IsInstanceOf(jnode, mpv_MPVNode_IntNode)) {
        jfieldID valueField = env->GetFieldID(mpv_MPVNode_IntNode, "value", "J");
        jlong int64 = env->GetLongField(jnode, valueField);
        node->format = MPV_FORMAT_INT64;
        node->u.int64 = int64;
        env->DeleteLocalRef(nodeClass);
        return 0;
    }

    if (env->IsInstanceOf(jnode, mpv_MPVNode_DoubleNode)) {
        jfieldID valueField = env->GetFieldID(mpv_MPVNode_DoubleNode, "value", "D");
        jdouble dbl = env->GetDoubleField(jnode, valueField);
        node->format = MPV_FORMAT_DOUBLE;
        node->u.double_ = dbl;
        env->DeleteLocalRef(nodeClass);
        return 0;
    }

    if (env->IsInstanceOf(jnode, mpv_MPVNode_ArrayNode)) {
        jfieldID valueField = env->GetFieldID(mpv_MPVNode_ArrayNode, "value", "[Lis/xyz/mpv/MPVNode;");
        jobjectArray jarray = (jobjectArray)env->GetObjectField(jnode, valueField);

        if (jarray) {
            jint size = env->GetArrayLength(jarray);

            node->format = MPV_FORMAT_NODE_ARRAY;
            node->u.list = (mpv_node_list*)malloc(sizeof(mpv_node_list));
            node->u.list->num = size;
            node->u.list->values = size > 0 ? (mpv_node*)calloc(size, sizeof(mpv_node)) : NULL;
            node->u.list->keys = NULL;

            for (int i = 0; i < size; i++) {
                jobject childNode = env->GetObjectArrayElement(jarray, i);
                if (childNode) {
                    jobject_to_mpv_node(env, childNode, &node->u.list->values[i]);
                    env->DeleteLocalRef(childNode);
                }
            }
        } else {
            node->format = MPV_FORMAT_NODE_ARRAY;
            node->u.list = (mpv_node_list*)malloc(sizeof(mpv_node_list));
            node->u.list->num = 0;
            node->u.list->values = NULL;
            node->u.list->keys = NULL;
        }
        env->DeleteLocalRef(nodeClass);
        return 0;
    }

    if (env->IsInstanceOf(jnode, mpv_MPVNode_MapNode)) {
        jfieldID valueField = env->GetFieldID(mpv_MPVNode_MapNode, "value", "Ljava/util/Map;");
        jobject jmap = env->GetObjectField(jnode, valueField);

        if (jmap) {
            jclass mapClass = env->GetObjectClass(jmap);
            jmethodID sizeMethod = env->GetMethodID(mapClass, "size", "()I");
            jmethodID entrySetMethod = env->GetMethodID(mapClass, "entrySet", "()Ljava/util/Set;");

            jint size = env->CallIntMethod(jmap, sizeMethod);
            jobject entrySet = env->CallObjectMethod(jmap, entrySetMethod);

            jclass setClass = env->GetObjectClass(entrySet);
            jmethodID toArrayMethod = env->GetMethodID(setClass, "toArray", "()[Ljava/lang/Object;");
            jobjectArray entryArray = (jobjectArray)env->CallObjectMethod(entrySet, toArrayMethod);

            node->format = MPV_FORMAT_NODE_MAP;
            node->u.list = (mpv_node_list*)malloc(sizeof(mpv_node_list));
            node->u.list->num = size;
            node->u.list->values = size > 0 ? (mpv_node*)calloc(size, sizeof(mpv_node)) : NULL;
            node->u.list->keys = size > 0 ? (char**)calloc(size, sizeof(char*)) : NULL;

            for (int i = 0; i < size; i++) {
                jobject entry = env->GetObjectArrayElement(entryArray, i);
                if (entry) {
                    jclass entryClass = env->GetObjectClass(entry);
                    jmethodID getKeyMethod = env->GetMethodID(entryClass, "getKey", "()Ljava/lang/Object;");
                    jmethodID getValueMethod = env->GetMethodID(entryClass, "getValue", "()Ljava/lang/Object;");

                    jstring keyStr = (jstring)env->CallObjectMethod(entry, getKeyMethod);
                    jobject valueObj = env->CallObjectMethod(entry, getValueMethod);

                    if (keyStr) {
                        const char *key = env->GetStringUTFChars(keyStr, NULL);
                        node->u.list->keys[i] = strdup(key);
                        env->ReleaseStringUTFChars(keyStr, key);
                        env->DeleteLocalRef(keyStr);
                    }

                    if (valueObj) {
                        jobject_to_mpv_node(env, valueObj, &node->u.list->values[i]);
                        env->DeleteLocalRef(valueObj);
                    }

                    env->DeleteLocalRef(entryClass);
                    env->DeleteLocalRef(entry);
                }
            }

            env->DeleteLocalRef(entryArray);
            env->DeleteLocalRef(setClass);
            env->DeleteLocalRef(entrySet);
            env->DeleteLocalRef(mapClass);
        } else {
            node->format = MPV_FORMAT_NODE_MAP;
            node->u.list = (mpv_node_list*)malloc(sizeof(mpv_node_list));
            node->u.list->num = 0;
            node->u.list->values = NULL;
            node->u.list->keys = NULL;
        }
        env->DeleteLocalRef(nodeClass);
        return 0;
    }

    env->DeleteLocalRef(nodeClass);
    return -1;
}

void free_mpv_node(mpv_node *node) {
    if (!node) return;

    switch (node->format) {
        case MPV_FORMAT_STRING:
            if (node->u.string) {
                free(node->u.string);
                node->u.string = NULL;
            }
            break;
        case MPV_FORMAT_NODE_ARRAY:
        case MPV_FORMAT_NODE_MAP:
            if (node->u.list) {
                for (int i = 0; i < node->u.list->num; i++)
                    free_mpv_node(&node->u.list->values[i]);

                if (node->format == MPV_FORMAT_NODE_MAP && node->u.list->keys) {
                    for (int i = 0; i < node->u.list->num; i++)
                        if (node->u.list->keys[i]) free(node->u.list->keys[i]);
                    free(node->u.list->keys);
                }

                if (node->u.list->values)
                    free(node->u.list->values);
                free(node->u.list);
                node->u.list = NULL;
            }
            break;
        default:
            break;
    }
    node->format = MPV_FORMAT_NONE;
}