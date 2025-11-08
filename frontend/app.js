const { createApp } = Vue;

const defaultOffering = () => ({ name: '', type: '产品', description: '' });
const defaultIntellectualProperty = () => ({ type: '', registrationNumber: '', description: '' });

createApp({
  data() {
    return {
      currentView: 'onboard',
      submitting: false,
      submitMessage: '',
      submitError: false,
      matching: false,
      matchQuery: '',
      matchResult: null,
      matchError: '',
      companyForm: {
        name: '',
        unifiedSocialCreditCode: '',
        establishmentDate: '',
        scale: '',
        industriesText: '',
        companyType: '',
        businessOverview: '',
        technologyStackText: '',
        address: {
          country: '',
          province: '',
          city: '',
          district: '',
          streetAddress: '',
        },
        coreOfferings: [defaultOffering()],
        intellectualProperties: [defaultIntellectualProperty()],
        contact: {
          name: '',
          title: '',
          phone: '',
          workEmail: '',
        },
        businessLicenseFileId: '',
        attachmentFileIds: [],
      },
      businessLicenseFileName: '',
      attachmentFiles: [],
    };
  },
  computed: {
    apiBase() {
      const base = window.API_BASE || '';
      return base.endsWith('/') ? base.slice(0, -1) : base;
    },
    businessLicenseFileId() {
      return this.companyForm.businessLicenseFileId;
    },
    formattedMatchResult() {
      return typeof this.matchResult === 'string'
        ? this.matchResult
        : JSON.stringify(this.matchResult, null, 2);
    },
  },
  methods: {
    addCoreOffering() {
      this.companyForm.coreOfferings.push(defaultOffering());
    },
    removeCoreOffering(index) {
      this.companyForm.coreOfferings.splice(index, 1);
    },
    addIntellectualProperty() {
      this.companyForm.intellectualProperties.push(defaultIntellectualProperty());
    },
    removeIntellectualProperty(index) {
      this.companyForm.intellectualProperties.splice(index, 1);
    },
    async handleBusinessLicenseUpload(event) {
      const file = event.target.files?.[0];
      if (!file) {
        return;
      }
      try {
        const metadata = await this.uploadFile(file);
        this.companyForm.businessLicenseFileId = metadata.id;
        this.businessLicenseFileName = metadata.filename || file.name;
        this.submitMessage = '营业执照上传成功';
        this.submitError = false;
      } catch (error) {
        console.error(error);
        this.submitMessage = '营业执照上传失败，请重试';
        this.submitError = true;
      }
    },
    async handleAttachmentUpload(event) {
      const files = Array.from(event.target.files || []);
      if (!files.length) {
        return;
      }
      for (const file of files) {
        try {
          const metadata = await this.uploadFile(file);
          this.companyForm.attachmentFileIds.push(metadata.id);
          this.attachmentFiles.push({ id: metadata.id, name: metadata.filename || file.name });
        } catch (error) {
          console.error(error);
          this.submitMessage = `附件 ${file.name} 上传失败`;
          this.submitError = true;
        }
      }
    },
    buildArrayFromText(text) {
      return text
        .split(',')
        .map((item) => item.trim())
        .filter((item) => item.length > 0);
    },
    compactOfferings() {
      return this.companyForm.coreOfferings
        .map((item) => ({ ...item }))
        .filter((item) => item.name || item.description);
    },
    compactIntellectualProperties() {
      return this.companyForm.intellectualProperties
        .map((item) => ({ ...item }))
        .filter((item) => item.type || item.registrationNumber || item.description);
    },
    async submitCompany() {
      this.submitting = true;
      this.submitMessage = '';
      this.submitError = false;

      const payload = {
        name: this.companyForm.name,
        unifiedSocialCreditCode: this.companyForm.unifiedSocialCreditCode,
        establishmentDate: this.companyForm.establishmentDate,
        scale: this.companyForm.scale,
        industries: this.buildArrayFromText(this.companyForm.industriesText),
        companyType: this.companyForm.companyType,
        businessOverview: this.companyForm.businessOverview,
        coreOfferings: this.compactOfferings(),
        technologyStack: this.buildArrayFromText(this.companyForm.technologyStackText),
        intellectualProperties: this.compactIntellectualProperties(),
        contact: { ...this.companyForm.contact },
        address: { ...this.companyForm.address },
        businessLicenseFileId: this.companyForm.businessLicenseFileId,
        attachmentFileIds: [...this.companyForm.attachmentFileIds],
      };

      try {
        const response = await fetch(`${this.apiBase}/api/companies`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        });

        if (!response.ok) {
          throw new Error('提交失败');
        }

        await response.json();
        this.submitMessage = '提交成功，已触发 Workflow 处理';
        this.resetCompanyForm();
      } catch (error) {
        console.error(error);
        this.submitMessage = '提交失败，请检查网络或后端配置';
        this.submitError = true;
      } finally {
        this.submitting = false;
      }
    },
    resetCompanyForm() {
      this.companyForm = {
        name: '',
        unifiedSocialCreditCode: '',
        establishmentDate: '',
        scale: '',
        industriesText: '',
        companyType: '',
        businessOverview: '',
        technologyStackText: '',
        address: {
          country: '',
          province: '',
          city: '',
          district: '',
          streetAddress: '',
        },
        coreOfferings: [defaultOffering()],
        intellectualProperties: [defaultIntellectualProperty()],
        contact: {
          name: '',
          title: '',
          phone: '',
          workEmail: '',
        },
        businessLicenseFileId: '',
        attachmentFileIds: [],
      };
      this.businessLicenseFileName = '';
      this.attachmentFiles = [];
    },
    async matchCompany() {
      if (!this.matchQuery.trim()) {
        this.matchError = '请输入问题描述';
        return;
      }

      this.matching = true;
      this.matchError = '';
      this.matchResult = null;

      try {
        const response = await fetch(`${this.apiBase}/api/companies/match?query=${encodeURIComponent(this.matchQuery)}`, {
          method: 'POST',
        });

        if (!response.ok) {
          throw new Error('匹配失败');
        }

        this.matchResult = await response.json();
      } catch (error) {
        console.error(error);
        this.matchError = '匹配失败，请稍后再试';
      } finally {
        this.matching = false;
      }
    },
    async uploadFile(file) {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(`${this.apiBase}/api/documents`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error('文件上传失败');
      }

      return response.json();
    },
  },
}).mount('#app');
