SUMMARY = "Install a InitRamFS enabled FitImage"
LICENSE = "MIT"
SRC_URI = " \
    file://checkfitimage.sh \
"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit kernel-artifact-names module-base

KERNEL_PRIORITY ?= "${@int(d.getVar('KERNEL_VERSION',1).split('-')[0].split('+')[0].split('.')[0]) * 10000 + \
                       int(d.getVar('KERNEL_VERSION',1).split('-')[0].split('+')[0].split('.')[1]) * 100 + \
                       int(d.getVar('KERNEL_VERSION',1).split('-')[0].split('+')[0].split('.')[-1])}"



PV="${KERNEL_VERSION}"


python do_package:prepend() {
    import oe.packagedata
    pkgv = oe.packagedata.read_subpkgdata('kernel-image-fitimage', d)['PKGV']
    d.setVar('PKGV', pkgv)
    pkgr = oe.packagedata.read_subpkgdata('kernel-image-fitimage', d)['PKGR']
    #d.setVar('PKGR', pkgr)
    bb.note("PKGV: %s" % d.getVar('PKGV'))
    bb.note("PKGR: %s" % d.getVar('PKGR'))
}

pkg_preinst:${PN} () {
    bootpart=`lsblk -o NAME,LABEL -r | grep "mmcblk[0-9]p[0-9] boot" | awk -F' ' '{print $1}'`
    mount /dev/$bootpart /boot || true
    dpkg-divert --package ${PN} --divert /usr/share/kernel/${KERNEL_IMAGETYPE}-initramfs-${KERNEL_VERSION} /boot/${KERNEL_IMAGETYPE}-initramfs-${KERNEL_VERSION}
}

pkg_postinst_ontarget:${PN} () {
    ${libexecdir}/checkfitimage.sh || true
}

pkg_prerm:${PN} () {
    dpkg-divert --remove /boot/${KERNEL_IMAGETYPE}-initramfs-${KERNEL_VERSION}
    ${libexecdir}/checkfitimage.sh || true
}

do_install() {
    install -d -m 0755 ${D}/boot/
    ls ${DEPLOY_DIR_IMAGE}/fitImage-*${KERNEL_FIT_BIN_EXT}
    install -m 0644 ${DEPLOY_DIR_IMAGE}/fitImage-${INITRAMFS_IMAGE}-${MACHINE}--${KERNEL_VERSION}*${KERNEL_FIT_BIN_EXT} ${D}/boot/${KERNEL_IMAGETYPE}-initramfs-${KERNEL_VERSION}
    install -d -m 0755 ${D}/${libexecdir}
    install -m 0755 ${WORKDIR}/checkfitimage.sh ${D}/${libexecdir}/
}

FILES:${PN} += " \
    /boot/${KERNEL_IMAGETYPE}-initramfs-${KERNEL_VERSION} \
    ${libexecdir}/checkfitimage.sh \
"

do_install[depends] = "virtual/kernel:do_deploy"

RDEPENDS:${PN} += " \
    u-boot-tools \
    kernel-modules-${PV} (>= ${PV}) \
"

RREPLACES:${PN} = " \
    kernel-image-fitimage \
"