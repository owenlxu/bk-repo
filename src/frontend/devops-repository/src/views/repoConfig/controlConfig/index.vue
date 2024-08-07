<template>
    <div>
        <bk-form class="control-config-container" :label-width="120" :model="controlConfigs" ref="controlForm" :rules="rules">
            <bk-form-item :label="$t('rootDirectoryPermission')">
                <bk-radio-group v-model="rootDirectoryPermission">
                    <bk-radio class="mr20" :value="true">{{ $t('enable') }}</bk-radio>
                    <bk-radio :value="false">{{ $t('disable') }}</bk-radio>
                    <i class="bk-icon icon-info f14 ml5" v-bk-tooltips="$t('rootDirectoryPermissionTip')"></i>
                </bk-radio-group>
            </bk-form-item>
            <template v-if="repoType === 'generic'">
                <bk-form-item v-for="type in genericInterceptorsList" :key="type"
                    :label="$t(`${type}Download`)" :property="`${type}.enable`">
                    <bk-radio-group v-model="controlConfigs[type].enable">
                        <bk-radio class="mr20" :value="true">{{ $t('enable') }}</bk-radio>
                        <bk-radio :value="false">{{ $t('disable') }}</bk-radio>
                    </bk-radio-group>
                    <template v-if="controlConfigs[type].enable && ['mobile', 'web'].includes(type)">
                        <bk-form-item :label="$t('fileName')" :label-width="80" class="mt10"
                            :property="`${type}.filename`" required error-display-type="normal">
                            <bk-input class="w250" v-model.trim="controlConfigs[type].filename"></bk-input>
                            <i class="bk-icon icon-info f14 ml5" v-bk-tooltips="$t('fileNameRule')"></i>
                        </bk-form-item>
                        <bk-form-item :label="$t('metadata')" :label-width="80"
                            :property="`${type}.metadata`" required error-display-type="normal">
                            <bk-input class="w250" v-model.trim="controlConfigs[type].metadata" :placeholder="$t('metadataRule')"></bk-input>
                            <a class="f12 ml5" href="https://docs.bkci.net/services/bkrepo/meta" target="__blank">{{ $t('viewMetadataDocument') }}</a>
                        </bk-form-item>
                    </template>
                    <template v-if="controlConfigs[type].enable && type === 'ip_segment'">
                        <bk-form-item :label="$t('IP')" :label-width="150" class="mt10"
                            :property="`${type}.ipSegment`" :required="!controlConfigs[type].officeNetwork" error-display-type="normal">
                            <bk-input class="w250 mr10" v-model.trim="controlConfigs[type].ipSegment" :placeholder="$t('ipPlaceholder')" :maxlength="4096"></bk-input>
                            <bk-checkbox v-model="controlConfigs[type].officeNetwork">{{ $t('office_networkDownload') }}</bk-checkbox>
                            <i class="bk-icon icon-info f14 ml5" v-bk-tooltips="$t('office_networkDownloadTips')"></i>
                        </bk-form-item>
                        <bk-form-item :label="$t('whiteUser')" :label-width="150"
                            :property="`${type}.whitelistUser`" error-display-type="normal">
                            <bk-input class="w250" v-model.trim="controlConfigs[type].whitelistUser" :placeholder="$t('whiteUserPlaceholder')"></bk-input>
                        </bk-form-item>
                    </template>
                </bk-form-item>
            </template>
            <bk-form-item :label="$t('blackUserList')" property="blackList" error-display-type="normal" v-if="isDevx">
                <div class="mb10 flex-between-center">
                    <bk-select
                        v-model="blackList"
                        class="bkre-user-select"
                        :multiple="true"
                        searchable
                        :placeholder="$t('controlConfigPlaceholder')">
                        <bk-option v-for="option in roleList"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name">
                        </bk-option>
                    </bk-select>
                    <bk-link theme="primary" @click="manageUserGroup" style="margin-right: auto;margin-left: 10px">{{ $t('userGroup') }}</bk-link>
                </div>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" @click="save()">{{$t('save')}}</bk-button>
            </bk-form-item>
        </bk-form>
        <add-user-dialog ref="addUserDialog" :visible.sync="showAddUserDialog" @complete="handleAddUsers"></add-user-dialog>
    </div>
</template>
<script>
    import { mapActions } from 'vuex'
    import AddUserDialog from '@/components/AddUserDialog/addUserDialog'
    import { specialRepoEnum } from '@/store/publicEnum'

    export default {
        name: 'controlConfig',
        components: { AddUserDialog },
        props: {
            baseData: Object
        },
        data () {
            const filenameRule = [
                {
                    required: true,
                    message: this.$t('pleaseFileName'),
                    trigger: 'blur'
                }
            ]
            const metadataRule = [
                {
                    required: true,
                    message: this.$t('pleaseMetadata'),
                    trigger: 'blur'
                },
                {
                    regex: /^[^\s]+:[^\s]+/,
                    message: this.$t('metadataRule'),
                    trigger: 'blur'
                }
            ]
            const ipSegmentRule = [
                {
                    required: true,
                    message: this.$t('pleaseIpSegment'),
                    trigger: 'blur'
                },
                {
                    validator: function (val) {
                        const ipList = val.split(',')
                        return ipList.every(ip => {
                            if (!ip) return true
                            return /(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\b\/([0-9]|[1-2][0-9]|3[0-2])\b)/.test(ip)
                        })
                    },
                    message: this.$t('ipSegmentRule'),
                    trigger: 'blur'
                }
            ]
            return {
                rootDirectoryPermission: false,
                controlConfigs: {
                    mobile: {
                        enable: false,
                        filename: '',
                        metadata: ''
                    },
                    web: {
                        enable: false,
                        filename: '',
                        metadata: ''
                    },
                    ip_segment: {
                        enable: false,
                        officeNetwork: false,
                        ipSegment: '',
                        whitelistUser: ''
                    }
                },
                blackList: [],
                showAddUserDialog: false,
                filenameRule,
                metadataRule,
                ipSegmentRule,
                roleList: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            repoName () {
                return this.$route.query.repoName
            },
            repoType () {
                return this.$route.params.repoType
            },
            isDevx () {
                return RELEASE_MODE === 'devx'
            },
            isTencent () {
                return RELEASE_MODE === 'tencent'
            },
            genericInterceptorsList () {
                return this.isTencent ? ['mobile', 'web', 'ip_segment'] : ['mobile', 'web']
            },
            rules () {
                return {
                    'mobile.filename': this.filenameRule,
                    'mobile.metadata': this.metadataRule,
                    'web.filename': this.filenameRule,
                    'web.metadata': this.metadataRule,
                    'ip_segment.ipSegment': this.controlConfigs.ip_segment.officeNetwork ? {} : this.ipSegmentRule
                }
            }
        },
        watch: {
            baseData: {
                handler (val) {
                    this.controlConfigs = val
                },
                deep: true,
                immediate: true
            }
        },
        created () {
            this.getRootPermission({
                projectId: this.projectId,
                repoName: this.repoName
            }).then((res) => {
                this.rootDirectoryPermission = res.status
                this.blackList = res.officeDenyGroupSet
            })
            this.getRoleListHandler()
        },
        methods: {
            ...mapActions(['updateRepoInfo', 'getRootPermission', 'getProjectRoleList', 'createOrUpdateRootPermission']),
            getRoleListHandler () {
                this.getProjectRoleList({ projectId: this.projectId }).then(res => {
                    res.forEach(role => {
                        this.roleList.push({
                            id: role.id,
                            name: role.name
                        })
                    })
                })
            },
            manageUserGroup () {
                this.$router.replace({
                    name: 'userGroup'
                })
            },
            addUserGroup () {
                this.$refs.roleForm.clearError()
                this.editRoleConfig = {
                    show: true,
                    loading: false,
                    id: '',
                    name: '',
                    description: '',
                    users: [],
                    originUsers: []
                }
            },
            showAddDialog () {
                this.showAddUserDialog = true
                this.$refs.addUserDialog.editUserConfig = {
                    users: this.editRoleConfig.users,
                    originUsers: this.editRoleConfig.originUsers,
                    search: '',
                    newUser: ''
                }
            },
            handleAddUsers (users) {
                this.editRoleConfig.originUsers = users
                this.editRoleConfig.users = users
            },
            deleteUser (index) {
                const temp = []
                for (let i = 0; i < this.editRoleConfig.users.length; i++) {
                    if (i !== index) {
                        temp.push(this.editRoleConfig.users[i])
                    }
                }
                this.editRoleConfig.users = temp
                this.editRoleConfig.originUsers = temp
            },
            async save () {
                await this.$refs.controlForm.validate()
                try {
                    await Promise.all([this.saveRepoConfig(), this.saveRepoMode()])
                    this.$emit('refresh')
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('save') + this.$t('space') + this.$t('success')
                    })
                } catch (e) {
                    this.$emit('refresh')
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('save') + this.$t('space') + this.$t('fail')
                    })
                }
            },
            saveRepoMode () {
                const body = {
                    projectId: this.projectId,
                    repoName: this.repoName,
                    controlEnable: this.rootDirectoryPermission,
                    officeDenyGroupSet: this.blackList
                }
                this.createOrUpdateRootPermission({
                    body: body
                })
            },
            saveRepoConfig () {
                const interceptors = []
                if (this.repoType === 'generic') {
                    ['mobile', 'web', 'ip_segment'].forEach(type => {
                        const { enable, filename, metadata, ipSegment, whitelistUser, officeNetwork } = this.baseData[type]
                        if (['mobile', 'web'].includes(type)) {
                            enable && interceptors.push({
                                type: type.toUpperCase(),
                                rules: { filename, metadata }
                            })
                        } else {
                            enable && interceptors.push({
                                type: type.toUpperCase(),
                                rules: {
                                    ipSegment: ipSegment.split(','),
                                    whitelistUser: this.isCommunity ? whitelistUser.split(',') : whitelistUser,
                                    officeNetwork
                                }
                            })
                        }
                    })
                }
                const body = {
                    public: this.baseData.public,
                    description: this.baseData.description,
                    display: this.baseData.display,
                    configuration: {
                        ...this.baseData.configuration,
                        settings: {
                            system: this.baseData.system,
                            interceptors: interceptors.length ? interceptors : undefined,
                            ...(
                                this.repoType === 'rpm'
                                    ? {
                                        enabledFileLists: this.baseData.enabledFileLists,
                                        repodataDepth: this.baseData.repodataDepth,
                                        groupXmlSet: this.baseData.groupXmlSet
                                    }
                                    : {}
                            )
                        }
                    }
                }
                if (!specialRepoEnum.includes(this.baseData.name)) {
                    body.configuration.settings.bkiamv3Check = this.baseData.configuration.settings.bkiamv3Check
                }
                this.updateRepoInfo({
                    projectId: this.projectId,
                    name: this.repoName,
                    body
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.control-config-container {
    .user-list {
        display: grid;
        grid-template: auto / repeat(4, 1fr);
        gap: 10px;
        max-height: 300px;
        max-width: 700px;
        overflow-y: auto;
        .user-item {
            height: 32px;
            border: 1px solid var(--borderWeightColor);
            background-color: var(--bgLighterColor);
            .user-name {
                max-width: 100px;
                margin-left: 5px;
            }
        }
    }
    .bkre-user-select {
        width: 300px;
        background-color: #FFFFFF1A;
        &:hover {
            background-color: rgba(255, 255, 255, 0.4);
        }
    }
}
.update-role-group-dialog {
    .bk-dialog-body {
        height: 500px;
    }
    ::v-deep .usersTextarea .bk-textarea-wrapper .bk-form-textarea{
        height: 500px;
    }
    .user-list {
        display: grid;
        grid-template: auto / repeat(3, 1fr);
        gap: 10px;
        max-height: 300px;
        overflow-y: auto;
        .user-item {
            height: 32px;
            border: 1px solid var(--borderWeightColor);
            background-color: var(--bgLighterColor);
            .user-name {
                max-width: 100px;
                margin-left: 5px;
            }
        }
    }
    .update-user-list {
        display: grid;
        grid-template: auto / repeat(1, 1fr);
        gap: 10px;
        max-height: 500px;
        overflow-y: auto;
        .update-user-item {
            height: 32px;
            border: 1px solid var(--borderWeightColor);
            background-color: var(--bgLighterColor);
            .update-user-name {
                max-width: 100px;
                margin-left: 5px;
            }
        }
    }
}
</style>
