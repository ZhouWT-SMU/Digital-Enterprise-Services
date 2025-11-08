const { createApp } = Vue;

createApp({
  data() {
    return {
      baseUrl: window.API_BASE || 'http://localhost:8080/api',
      submitting: false,
      matching: false,
      statusMessage: '',
      statusError: false,
      matchStatus: '',
      matchError: false,
      matchResult: '',
      matchQuery: '',
      attachments: [],
      form: {
        name: '',
        unifiedSocialCreditCode: '',
        establishmentDate: '',
        address: {
          country: '',
          province: '',
          city: '',
          district: '',
          streetAddress: ''
        },
        scale: '',
        industriesInput: '',
        companyType: '',
        businessOverview: '',
        coreOfferings: [createEmptyOffering()],
        technologyStackInput: '',
        intellectualProperties: [createEmptyIp()],
        contact: {
          name: '',
          title: '',
          phone: '',
          workEmail: ''
        },
        businessLicenseFileId: '',
        businessLicenseFileName: '',
        attachmentFileIds: []
      }
    };
  },
  computed: {
    apiBase() {
      return this.baseUrl;
    }
  },
  methods: {
    normalizeList(text) {
      return (text || '')
        .split(/[,\n]/)
        .map((item) => item.trim())
        .filter(Boolean);
    },
    addCoreOffering() {
      this.form.coreOfferings.push(createEmptyOffering());
    },
    removeCoreOffering(index) {
      this.form.coreOfferings.splice(index, 1);
      if (!this.form.coreOfferings.length) {
        this.form.coreOfferings.push(createEmptyOffering());
      }
    },
    addIp() {
      this.form.intellectualProperties.push(createEmptyIp());
    },
    removeIp(index) {
      this.form.intellectualProperties.splice(index, 1);
      if (!this.form.intellectualProperties.length) {
        this.form.intellectualProperties.push(createEmptyIp());
      }
    },
    resetStatus() {
      this.statusMessage = '';
      this.statusError = false;
    },
    async submitForm() {
      this.resetStatus();
      if (!this.form.businessLicenseFileId) {
        this.statusMessage = '请先上传营业执照文件。';
        this.statusError = true;
        return;
      }

      const industries = this.normalizeList(this.form.industriesInput);
      if (!industries.length) {
        this.statusMessage = '所属行业不能为空。';
        this.statusError = true;
        return;
      }

      const coreOfferings = this.form.coreOfferings
        .filter((item) => item.name && item.type && item.description)
        .map((item) => ({
          name: item.name,
          type: item.type,
          description: item.description
        }));
      if (!coreOfferings.length) {
        this.statusMessage = '请至少填写一个核心产品/服务。';
        this.statusError = true;
        return;
      }

      const intellectualProperties = this.form.intellectualProperties
        .filter((item) => item.type || item.registrationNumber || item.description)
        .map((item) => ({
          type: item.type,
          registrationNumber: item.registrationNumber,
          description: item.description
        }));

      const payload = {
        name: this.form.name,
        unifiedSocialCreditCode: this.form.unifiedSocialCreditCode,
        establishmentDate: this.form.establishmentDate,
        address: this.form.address,
        scale: this.form.scale,
        industries,
        companyType: this.form.companyType,
        businessOverview: this.form.businessOverview,
        coreOfferings,
        technologyStack: this.normalizeList(this.form.technologyStackInput),
        intellectualProperties,
        contact: this.form.contact,
        businessLicenseFileId: this.form.businessLicenseFileId,
        attachmentFileIds: this.form.attachmentFileIds
      };

      this.submitting = true;
      try {
        const response = await fetch(`${this.apiBase}/companies`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(payload)
        });

        if (!response.ok) {
          const errorText = await response.text();
          throw new Error(errorText || '提交失败');
        }

        this.statusMessage = '企业信息提交成功。';
        this.resetFormAfterSubmit();
      } catch (error) {
        this.statusMessage = error.message || '提交失败，请稍后重试。';
        this.statusError = true;
      } finally {
        this.submitting = false;
      }
    },
    resetFormAfterSubmit() {
      this.form.name = '';
      this.form.unifiedSocialCreditCode = '';
      this.form.establishmentDate = '';
      this.form.address = {
        country: '',
        province: '',
        city: '',
        district: '',
        streetAddress: ''
      };
      this.form.scale = '';
      this.form.industriesInput = '';
      this.form.companyType = '';
      this.form.businessOverview = '';
      this.form.coreOfferings = [createEmptyOffering()];
      this.form.technologyStackInput = '';
      this.form.intellectualProperties = [createEmptyIp()];
      this.form.contact = {
        name: '',
        title: '',
        phone: '',
        workEmail: ''
      };
      this.form.businessLicenseFileId = '';
      this.form.businessLicenseFileName = '';
      this.form.attachmentFileIds = [];
      this.attachments = [];
    },
    async handleLicenseUpload(event) {
      const file = event.target.files?.[0];
      if (!file) {
        return;
      }
      try {
        const metadata = await this.uploadDocument(file);
        this.form.businessLicenseFileId = metadata.id;
        this.form.businessLicenseFileName = metadata.filename || file.name;
        this.statusMessage = `营业执照上传成功（ID: ${metadata.id}）`;
      } catch (error) {
        this.statusMessage = error.message || '营业执照上传失败';
        this.statusError = true;
      }
    },
    async handleAttachmentUpload(event) {
      const files = Array.from(event.target.files || []);
      if (!files.length) {
        return;
      }
      for (const file of files) {
        try {
          const metadata = await this.uploadDocument(file);
          this.form.attachmentFileIds.push(metadata.id);
          this.attachments.push({ id: metadata.id, name: metadata.filename || file.name });
        } catch (error) {
          this.statusMessage = `附件 ${file.name} 上传失败：${error.message}`;
          this.statusError = true;
        }
      }
      event.target.value = '';
    },
    async uploadDocument(file) {
      const formData = new FormData();
      formData.append('file', file);
      const response = await fetch(`${this.apiBase}/documents`, {
        method: 'POST',
        body: formData
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || '上传失败');
      }
      return response.json();
    },
    async runMatch() {
      this.matchStatus = '';
      this.matchError = false;
      this.matchResult = '';
      if (!this.matchQuery.trim()) {
        this.matchStatus = '请输入匹配问题。';
        this.matchError = true;
        return;
      }
      this.matching = true;
      try {
        const response = await fetch(`${this.apiBase}/companies/match?query=${encodeURIComponent(this.matchQuery)}`, {
          method: 'POST'
        });
        if (!response.ok) {
          const text = await response.text();
          throw new Error(text || '匹配失败');
        }
        const result = await response.json();
        this.matchResult = JSON.stringify(result, null, 2);
        this.matchStatus = '匹配完成。';
      } catch (error) {
        this.matchStatus = error.message || '匹配失败，请稍后重试。';
        this.matchError = true;
      } finally {
        this.matching = false;
      }
    }
  }
}).mount('#app');

function createEmptyOffering() {
  return {
    name: '',
    type: '产品',
    description: ''
  };
}

function createEmptyIp() {
  return {
    type: '',
    registrationNumber: '',
    description: ''
  };
}
